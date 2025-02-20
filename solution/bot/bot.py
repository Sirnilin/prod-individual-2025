import asyncio
import logging
import os
import uuid
import aiohttp
import aiosqlite
from aiogram import Bot, Dispatcher, types
from aiogram.filters import Command
from aiogram.types import Message, ReplyKeyboardMarkup, KeyboardButton, InlineKeyboardMarkup, InlineKeyboardButton

API_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
BACKEND_URL = os.getenv("BACKEND_URL")
DATABASE = "clients.db"

bot = Bot(token=API_TOKEN)
dp = Dispatcher()

# Хранение ролей пользователей (client/advertiser) и выбранных клиентов
user_roles = {}  # {user_id: "client" или "advertiser"}
selected_clients = {}

async def init_db():
    async with aiosqlite.connect(DATABASE) as db:
        await db.execute(
            """
            CREATE TABLE IF NOT EXISTS clients (
                client_id TEXT PRIMARY KEY,
                login TEXT NOT NULL,
                age INTEGER NOT NULL,
                location TEXT NOT NULL,
                gender TEXT NOT NULL,
                user_id INTEGER NOT NULL
            );
            """
        )
        await db.commit()

@dp.message(Command("start"))
async def start_command(message: Message):
    # Предлагаем выбрать роль
    keyboard = InlineKeyboardMarkup(inline_keyboard=[
        [InlineKeyboardButton(text="Клиент", callback_data="role:client")],
        [InlineKeyboardButton(text="Рекламодатель", callback_data="role:advertiser")]
    ])
    await message.answer("Выберите вашу роль:", reply_markup=keyboard)

@dp.callback_query(lambda c: c.data.startswith("role:"))
async def set_role(callback: types.CallbackQuery):
    role = callback.data.split(":")[1]
    user_id = callback.from_user.id
    user_roles[user_id] = role
    # Формируем меню в зависимости от роли
    if role == "client":
        buttons = ["Создать клиента", "Список клиентов", "Выбрать клиента", "Получить рекламу"]
    else:
        buttons = ["Создать рекламодателя", "Создать рекламу", "Сгенерировать текст рекламы", "Изменить рекламу"]
    keyboard = ReplyKeyboardMarkup(
        keyboard=[[KeyboardButton(text=b)] for b in buttons],
        resize_keyboard=True
    )
    await callback.message.answer(f"Вы выбрали роль: {role}.", reply_markup=keyboard)

# -------------------------
# Обработчики для клиента
# -------------------------
@dp.message(lambda msg: msg.text == "Создать клиента" and user_roles.get(msg.from_user.id) == "client")
async def create_client(message: Message):
    user_id = message.from_user.id
    # Проверяем, сколько клиентов уже создано
    async with aiosqlite.connect(DATABASE) as db:
        cursor = await db.execute("SELECT COUNT(*) FROM clients WHERE user_id = ?", (user_id,))
        client_count = (await cursor.fetchone())[0]
    if client_count >= 10:
        await message.answer("Вы уже создали максимальное количество клиентов (10).")
        return
    await message.answer(
        "Введите данные клиента в формате:\n"
        "`Имя:TestUser\nВозраст:25\nГород:Москва\nПол:MALE`"
    )

@dp.message(lambda msg: "Имя:" in msg.text and "Возраст:" in msg.text and user_roles.get(msg.from_user.id) == "client")
async def save_client(message: Message):
    lines = message.text.split("\n")
    data_map = {}
    for line in lines:
        key_val = line.split(":", 1)
        if len(key_val) == 2:
            data_map[key_val[0].strip()] = key_val[1].strip()
    new_id = str(uuid.uuid4())
    client_data = {
        "client_id": new_id,
        "login": data_map.get("Имя", "Unknown"),
        "age": int(data_map.get("Возраст", 0)),
        "location": data_map.get("Город", "Unknown"),
        "gender": data_map.get("Пол", "MALE"),
        "user_id": message.from_user.id
    }
    async with aiosqlite.connect(DATABASE) as db:
        await db.execute(
            "INSERT INTO clients (client_id, login, age, location, gender, user_id) VALUES (?, ?, ?, ?, ?, ?)",
            (client_data["client_id"], client_data["login"], client_data["age"], client_data["location"], client_data["gender"], client_data["user_id"])
        )
        await db.commit()
    response = await send_client_to_microservice(client_data)
    if response:
        await message.answer(f"Клиент создан с ID: {new_id} и отправлен в микросервис.")
    else:
        await message.answer(f"Клиент создан с ID: {new_id}, но не удалось отправить в микросервис.")

@dp.message(lambda msg: msg.text == "Список клиентов" and user_roles.get(msg.from_user.id) == "client")
async def list_clients(message: Message):
    user_id = message.from_user.id
    async with aiosqlite.connect(DATABASE) as db:
        cursor = await db.execute("SELECT client_id, login FROM clients WHERE user_id = ? LIMIT 10", (user_id,))
        clients = await cursor.fetchall()
    if not clients:
        await message.answer("Пока нет клиентов.")
        return
    text_list = [f"{i + 1}. {login}" for i, (_, login) in enumerate(clients)]
    await message.answer("Список клиентов (макс. 10):\n" + "\n".join(text_list))

@dp.message(lambda msg: msg.text == "Выбрать клиента" and user_roles.get(msg.from_user.id) == "client")
async def prompt_select_client(message: Message):
    user_id = message.from_user.id
    async with aiosqlite.connect(DATABASE) as db:
        cursor = await db.execute("SELECT client_id, login FROM clients WHERE user_id = ? LIMIT 10", (user_id,))
        clients = await cursor.fetchall()
    if not clients:
        await message.answer("У вас нет клиентов для выбора.")
        return
    buttons = [InlineKeyboardButton(text=f"{i + 1}", callback_data=f"select_client:{client_id}") for i, (client_id, _) in enumerate(clients)]
    keyboard = InlineKeyboardMarkup(inline_keyboard=[buttons[i:i + 5] for i in range(0, len(buttons), 5)])
    await message.answer("Выберите клиента:", reply_markup=keyboard)

@dp.callback_query(lambda c: c.data.startswith("select_client:"))
async def set_selected_client(callback: types.CallbackQuery):
    user_id = callback.from_user.id
    client_id = callback.data.split(":")[1]
    async with aiosqlite.connect(DATABASE) as db:
        cursor = await db.execute("SELECT client_id FROM clients WHERE client_id = ? AND user_id = ?", (client_id, user_id))
        client = await cursor.fetchone()
    if client:
        selected_clients[user_id] = client_id
        await callback.message.answer(f"Выбран клиент: {client_id}")
    else:
        await callback.message.answer("Такого клиента не существует.")

@dp.message(lambda msg: msg.text == "Получить рекламу" and user_roles.get(msg.from_user.id) == "client")
async def get_ads(message: Message):
    user_id = message.from_user.id
    client_id = selected_clients.get(user_id)
    if not client_id:
        await message.answer("Сначала выберите клиента.")
        return
    async with aiohttp.ClientSession() as session:
        url = f"{BACKEND_URL}/ads?client_id={client_id}"
        async with session.get(url) as resp:
            if resp.status == 200:
                ad_data = await resp.json()
                text_response = (
                    f"Название: {ad_data.get('ad_title')}\n"
                    f"Текст: {ad_data.get('ad_text')}\n"
                    f"ID кампании: {ad_data.get('ad_id')}\n"
                    f"ID рекламодателя: {ad_data.get('advertiser_id')}"
                )
                await message.answer(text_response)
            elif resp.status == 404:
                await message.answer("Нет доступной рекламы для данного клиента.")
            else:
                await message.answer(f"Ошибка при получении рекламы. Код: {resp.status}")

# -----------------------------
# Обработчики для рекламодателя
# -----------------------------
@dp.message(lambda msg: msg.text == "Создать рекламодателя" and user_roles.get(msg.from_user.id) == "advertiser")
async def create_advertiser(message: Message):
    await message.answer("Введите данные рекламодателя в формате:\n`Имя:TestAdvertiser\nКатегория:Tech`")
    # Здесь можно реализовать сохранение данных рекламодателя (например, в отдельной таблице)

@dp.message(lambda msg: msg.text == "Создать рекламу" and user_roles.get(msg.from_user.id) == "advertiser")
async def create_ad(message: Message):
    await message.answer("Введите данные рекламы в формате:\n`Название:Awesome Ad\nОписание:This is an ad description`")
    # Реализуйте логику создания рекламы

@dp.message(lambda msg: msg.text == "Сгенерировать текст рекламы" and user_roles.get(msg.from_user.id) == "advertiser")
async def generate_ad_text(message: Message):
    # Пример генерации текста (можно интегрировать с внешним сервисом)
    generated_text = "Супер рекламное сообщение!"
    await message.answer(f"Сгенерированный текст рекламы: {generated_text}")

@dp.message(lambda msg: msg.text == "Изменить рекламу" and user_roles.get(msg.from_user.id) == "advertiser")
async def modify_ad(message: Message):
    await message.answer("Введите ID рекламы и новые данные в формате:\n`ID:12345\nНазвание:New Title\nОписание:New description`")
    # Реализуйте логику изменения данных рекламы

async def send_client_to_microservice(client_data):
    async with aiohttp.ClientSession() as session:
        url = f"{BACKEND_URL}/clients/bulk"
        async with session.post(url, json=[client_data]) as resp:
            if resp.status == 201:
                return await resp.json()
            else:
                return None

async def main():
    logging.basicConfig(level=logging.INFO)
    await init_db()
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())
