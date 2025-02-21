import asyncio
from io import BytesIO
import matplotlib.pyplot as plt
from aiogram.types import InputFile
import json
import logging
import os
import uuid
import aiohttp
import aiosqlite
from aiogram import Bot, Dispatcher, types
from aiogram.filters import Command
from aiogram.types import Message, ReplyKeyboardMarkup, KeyboardButton, InlineKeyboardMarkup, InlineKeyboardButton

API_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "7943694592:AAE1nGTCMqFjTYIsdPg-LflfBOPOzpqJeqA")
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
DATABASE = "clients.db"

bot = Bot(token=API_TOKEN)
dp = Dispatcher()

user_roles = {}
selected_clients = {}
selected_advertisers = {}
pending_ad_text_generation = {}

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
        buttons = [
            "🆕 Создать клиента",
            "📋 Список клиентов",
            "✅ Выбрать клиента",
            "🎯 Получить рекламу"
        ]
    else:
        buttons = [
            "🏢 Создать рекламодателя",
            "📢 Создать рекламу",
            "✍️ Сгенерировать текст рекламы",
            "🛠 Изменить рекламу",
            "📊 График статистики"
        ]

    keyboard = ReplyKeyboardMarkup(
        keyboard=[[KeyboardButton(text=b)] for b in buttons],
        resize_keyboard=True
    )

    await callback.message.answer(f"✅ Вы выбрали роль: `{role}`.", reply_markup=keyboard)


# -------------------------
# Обработчики для клиента
# -------------------------
@dp.message(lambda msg: msg.text == "🆕 Создать клиента" and user_roles.get(msg.from_user.id) == "client")
async def create_client(message: Message):
    user_id = message.from_user.id
    async with aiosqlite.connect(DATABASE) as db:
        cursor = await db.execute("SELECT COUNT(*) FROM clients WHERE user_id = ?", (user_id,))
        client_count = (await cursor.fetchone())[0]
    if client_count >= 10:
        await message.answer("❌ Вы уже создали максимальное количество клиентов (10).")
        return
    await message.answer(
        "🔹 Введите данные клиента в следующем формате:\n\n"
        "🔹 `name:TestUser` - имя клиента\n"
        "🔹 `age:25` - возраст клиента\n"
        "🔹 `city:Москва` - город проживания\n"
        "🔹 `gender:male` - пол (MALE, FEMALE)\n\n"
        "📌 Пример ввода:\n"
        "```\n"
        "name:Иван\n"
        "age:30\n"
        "city:Москва\n"
        "gender:MALE\n"
        "```"
    )

@dp.message(lambda msg: "name:" in msg.text and "age:" in msg.text and user_roles.get(msg.from_user.id) == "client")
async def save_client(message: Message):
    lines = message.text.split("\n")
    data_map = {}
    for line in lines:
        key_val = line.split(":", 1)
        if len(key_val) == 2:
            data_map[key_val[0].strip().lower()] = key_val[1].strip()  # Приводим ключи к нижнему регистру

    new_id = str(uuid.uuid4())
    client_data = {
        "client_id": new_id,
        "login": data_map.get("name", "Unknown"),
        "age": int(data_map.get("age", 0)),
        "location": data_map.get("city", "Unknown"),
        "gender": data_map.get("gender", "male"),
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
        await message.answer(f"✅ Клиент создан с ID: `{new_id}`")
    else:
        await message.answer(f"⚠️ Ошибка при создании клиента.")

@dp.message(lambda msg: msg.text == "📋 Список клиентов" and user_roles.get(msg.from_user.id) == "client")
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

@dp.message(lambda msg: msg.text == "✅ Выбрать клиента" and user_roles.get(msg.from_user.id) == "client")
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

@dp.message(lambda msg: msg.text == "🎯 Получить рекламу" and user_roles.get(msg.from_user.id) == "client")
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
                # Формируем текст объявления
                text_response = (
                    f"Название: {ad_data.get('ad_title')}\n"
                    f"Текст: {ad_data.get('ad_text')}\n"
                    f"ID кампании: {ad_data.get('ad_id')}\n"
                    f"ID рекламодателя: {ad_data.get('advertiser_id')}"
                )

                keyboard = InlineKeyboardMarkup(inline_keyboard=[
                    [InlineKeyboardButton(text="Кликнуть", callback_data=f"click_ad:{ad_data.get('ad_id')}")]
                ])
                await message.answer(text_response, reply_markup=keyboard)
            elif resp.status == 404:
                await message.answer("Нет доступной рекламы для данного клиента.")
            else:
                await message.answer(f"Ошибка при получении рекламы. Код: {resp.status}")

@dp.callback_query(lambda c: c.data.startswith("click_ad:"))
async def process_ad_click(callback: types.CallbackQuery):
    ad_id = callback.data.split(":")[1]
    user_id = callback.from_user.id
    client_id = selected_clients.get(user_id)
    if not client_id:
        await callback.message.answer("Клиент не выбран.")
        return
    click_url = f"{BACKEND_URL}/ads/{ad_id}/click"
    payload = {"client_id": client_id}
    async with aiohttp.ClientSession() as session:
        async with session.post(click_url, json=payload) as resp:
            if resp.status == 204:
                await callback.message.answer("Клик зафиксирован.")
            else:
                error_text = await resp.text()
                await callback.message.answer(f"Ошибка при клике: {error_text}")

# -----------------------------
# Обработчики для рекламодателя
# -----------------------------
async def send_advertiser_to_backend(advertiser_data: dict):
    """Отправка данных рекламодателя на эндпоинт bulk insert."""
    async with aiohttp.ClientSession() as session:
        url = f"{BACKEND_URL}/advertisers/bulk"
        async with session.post(url, json=[advertiser_data]) as resp:
            if resp.status == 201:
                return await resp.json()
            else:
                return None

async def create_campaign_on_backend(advertiser_id: str, campaign_request: dict):
    """Создание кампании для заданного рекламодателя."""
    async with aiohttp.ClientSession() as session:
        url = f"{BACKEND_URL}/advertisers/{advertiser_id}/campaigns"
        async with session.post(url, json=campaign_request) as resp:
            if resp.status == 201:
                return await resp.json()
            else:
                return None

async def update_campaign_on_backend(advertiser_id: str, campaign_id: str, campaign_update: dict):
    """Обновление кампании для заданного рекламодателя."""
    async with aiohttp.ClientSession() as session:
        url = f"{BACKEND_URL}/advertisers/{advertiser_id}/campaigns/{campaign_id}"
        async with session.put(url, json=campaign_update) as resp:
            if resp.status == 200:
                return await resp.json()
            else:
                return None

@dp.message(lambda msg: msg.text == "🏢 Создать рекламодателя" and user_roles.get(msg.from_user.id) == "advertiser")
async def create_advertiser(message: Message):
    await message.answer("Введите данные рекламодателя в формате:\n`Имя:TestAdvertiser`")

@dp.message(lambda msg: "Имя:" in msg.text and user_roles.get(msg.from_user.id) == "advertiser")
async def save_advertiser(message: Message):
    # Простой парсинг входных данных
    data = message.text.split("\n")
    data_map = {}
    for line in data:
        if ":" in line:
            key, value = line.split(":", 1)
            data_map[key.strip()] = value.strip()
    new_id = str(uuid.uuid4())
    advertiser_data = {
        "advertiser_id": new_id,
        "name": data_map.get("Имя", "Unknown")
    }
    response = await send_advertiser_to_backend(advertiser_data)
    if response:
        # Сохраняем выбранного рекламодателя, если необходимо
        selected_advertisers[message.from_user.id] = new_id
        await message.answer(f"Рекламодатель создан с ID: {new_id}")
    else:
        await message.answer("Ошибка при создании рекламодателя.")

@dp.message(lambda msg: msg.text == "📢 Создать рекламу" and user_roles.get(msg.from_user.id) == "advertiser")
async def create_campaign(message: Message):
    user_id = message.from_user.id
    advertiser_id = selected_advertisers.get(user_id)
    if not advertiser_id:
        await message.answer("Сначала создайте или выберите рекламодателя.")
        return
    await message.answer(
        "Введите данные кампании в следующем формате:\n\n"
        "🔹 `impressions_limit:1000` - лимит показов\n"
        "🔹 `clicks_limit:100` - лимит кликов\n"
        "🔹 `cost_per_impression:0.05` - цена за 1 показ (в у.е.)\n"
        "🔹 `cost_per_click:0.5` - цена за 1 клик (в у.е.)\n"
        "🔹 `ad_title:Название рекламы` - заголовок объявления\n"
        "🔹 `ad_text:Описание рекламы` - текст объявления\n"
        "🔹 `start_date:1672531200` - дата начала \n"
        "🔹 `end_date:1672617600` - дата окончания \n"
        "🔹 `target_age_from:18` - минимальный возраст аудитории (необязательно)\n"
        "🔹 `target_age_to:35` - максимальный возраст аудитории (необязательно)\n"
        "🔹 `target_location:Москва` - геотаргетинг (необязательно)\n"
        "🔹 `target_gender:male` - пол (MALE, FEMALE, ALL) (необязательно)\n\n"
        "📌 Пример ввода:\n"
        "```\n"
        "impressions_limit:1000\n"
        "clicks_limit:100\n"
        "cost_per_impression:0.05\n"
        "cost_per_click:0.5\n"
        "ad_title:Супер скидки!\n"
        "ad_text:Только сегодня скидки до 50%!\n"
        "start_date:1714060800\n"
        "end_date:1714233600\n"
        "target_age_from:18\n"
        "target_age_to:35\n"
        "target_location:Москва\n"
        "target_gender:ALL\n"
        "```"
    )

@dp.message(lambda msg: msg.text.startswith("impressions_limit:") and user_roles.get(msg.from_user.id) == "advertiser")
async def save_campaign(message: Message):
    user_id = message.from_user.id
    advertiser_id = selected_advertisers.get(user_id)
    if not advertiser_id:
        await message.answer("Сначала создайте рекламодателя.")
        return

    lines = message.text.split("\n")
    data_map = {}
    for line in lines:
        if ":" in line:
            key, value = line.split(":", 1)
            data_map[key.strip()] = value.strip()

    # Формируем таргетинг
    targeting = {
        "age_from": int(data_map.get("target_age_from", 0)),
        "age_to": int(data_map.get("target_age_to", 0)),
        "location": data_map.get("target_location", ""),
        "gender": data_map.get("target_gender", "")
    }

    campaign_request = {
        "impressions_limit": int(data_map.get("impressions_limit", 0)),
        "clicks_limit": int(data_map.get("clicks_limit", 0)),
        "cost_per_impression": float(data_map.get("cost_per_impression", 0)),
        "cost_per_click": float(data_map.get("cost_per_click", 0)),
        "ad_title": data_map.get("ad_title", "Без названия"),
        "ad_text": data_map.get("ad_text", ""),
        "start_date": int(data_map.get("start_date", 0)),
        "end_date": int(data_map.get("end_date", 0)),
        "targeting": targeting
    }

    response = await create_campaign_on_backend(advertiser_id, campaign_request)
    if response:
        await message.answer(f"Кампания создана:\nID: {response.get('campaign_id')}")
    else:
        await message.answer("Ошибка при создании кампании.")

@dp.message(lambda msg: msg.text == "🛠 Изменить рекламу" and user_roles.get(msg.from_user.id) == "advertiser")
async def modify_campaign(message: Message):
    await message.answer(
        "Введите данные для изменения кампании в следующем формате:\n\n"
        "🔹 `campaign_id: ID_Кампании` - ID кампании для обновления\n"
        "🔹 `cost_per_impression: 0.04` - новая цена за показ\n"
        "🔹 `cost_per_click: 0.4` - новая цена за клик\n"
        "🔹 `ad_title: Новый заголовок` - новый заголовок рекламы\n"
        "🔹 `ad_text: Новый текст` - новый текст рекламы\n"
        "🔹 `start_date: 1672531200` - новое время начала (UNIX timestamp)\n"
        "🔹 `end_date: 1672617600` - новое время окончания (UNIX timestamp)\n"
        "🔹 `target_age_from: 18` - минимальный возраст аудитории (необязательно)\n"
        "🔹 `target_age_to: 35` - максимальный возраст аудитории (необязательно)\n"
        "🔹 `target_location: Москва` - геотаргетинг (необязательно)\n"
        "🔹 `target_gender: ALL` - пол аудитории (MALE, FEMALE, ALL) (необязательно)\n\n"
        "📌 Пример ввода:\n"
        "```\n"
        "campaign_id: 12345\n"
        "cost_per_impression: 0.05\n"
        "cost_per_click: 0.5\n"
        "ad_title: Новая реклама\n"
        "ad_text: Обновленное описание рекламы\n"
        "start_date: 1714060800\n"
        "end_date: 1714233600\n"
        "target_age_from: 18\n"
        "target_age_to: 35\n"
        "target_location: Москва\n"
        "target_gender: ALL\n"
        "```"
    )

@dp.message(lambda msg: "campaign_id:" in msg.text and user_roles.get(msg.from_user.id) == "advertiser")
async def save_campaign_update(message: Message):
    user_id = message.from_user.id
    advertiser_id = selected_advertisers.get(user_id)
    if not advertiser_id:
        await message.answer("Сначала создайте рекламодателя.")
        return

    lines = message.text.split("\n")
    data_map = {}
    for line in lines:
        if ":" in line:
            key, value = line.split(":", 1)
            data_map[key.strip()] = value.strip()

    campaign_id = data_map.get("campaign_id")
    if not campaign_id:
        await message.answer("Не указан campaign_id.")
        return

    try:
        campaign_update = {
            "cost_per_impression": float(data_map.get("cost_per_impression", 0)),
            "cost_per_click": float(data_map.get("cost_per_click", 0)),
            "ad_title": data_map.get("ad_title", ""),
            "ad_text": data_map.get("ad_text", ""),
            "start_date": int(data_map.get("start_date", 0)),
            "end_date": int(data_map.get("end_date", 0)),
            "targeting": {
                "age_from": int(data_map.get("target_age_from", 0)),
                "age_to": int(data_map.get("target_age_to", 0)),
                "location": data_map.get("target_location", ""),
                "gender": data_map.get("target_gender", "")
            }
        }
    except ValueError as e:
        await message.answer(f"Ошибка в данных: {e}")
        return

    response = await update_campaign_on_backend(advertiser_id, campaign_id, campaign_update)
    if response:
        await message.answer(f"Кампания обновлена: {response}")
    else:
        await message.answer("Ошибка при обновлении кампании.")


@dp.message(
    lambda msg: msg.text == "✍️ Сгенерировать текст рекламы" and user_roles.get(msg.from_user.id) == "advertiser")
async def prompt_ad_text_generation(message: Message):
    advertiser_id = selected_advertisers.get(message.from_user.id)
    if not advertiser_id:
        await message.answer("Сначала создайте или выберите рекламодателя.")
        return

    pending_ad_text_generation[message.from_user.id] = True
    await message.answer("Пожалуйста, введите описание вашей рекламной кампании:")


@dp.message(
    lambda msg: msg.from_user.id in pending_ad_text_generation and user_roles.get(msg.from_user.id) == "advertiser")
async def process_ad_text_generation(message: Message):

    pending_ad_text_generation.pop(message.from_user.id, None)

    advertiser_id = selected_advertisers.get(message.from_user.id)
    if not advertiser_id:
        await message.answer("Сначала создайте или выберите рекламодателя.")
        return

    user_description = message.text

    generate_request = {
        "campaign_description": user_description
    }

    async with aiohttp.ClientSession() as session:
        url = f"{BACKEND_URL}/gigachat/generateAdText"
        async with session.get(url, json=generate_request) as resp:
            if resp.status == 200:
                content_type = resp.headers.get('Content-Type', '')
                if 'application/json' in content_type:
                    response_json = await resp.json()
                    generated_text = response_json.get("ad_text", "Текст не найден")
                    await message.answer(f"Сгенерированный текст рекламы:\n{generated_text}")
                else:
                    text_response = await resp.text()
                    try:
                        response_json = json.loads(text_response)
                        generated_text = response_json.get("ad_text", "Текст не найден")
                        await message.answer(f"Сгенерированный текст рекламы:\n{generated_text}")
                    except json.JSONDecodeError:
                        await message.answer(f"Получен неожиданный ответ:\n{text_response}")
            else:
                await message.answer("Ошибка при генерации текста рекламы.")


@dp.message(lambda msg: msg.text == "📊 График статистики")
async def get_statistics(message: Message):
    advertiser_id = selected_advertisers.get(message.from_user.id)
    if not advertiser_id:
        await message.answer("Сначала создайте или выберите рекламодателя.")
        return

    async with aiohttp.ClientSession() as session:
        url = f"{BACKEND_URL}/stats/advertisers/{advertiser_id}/campaigns"
        async with session.get(url) as resp:
            if resp.status == 200:
                stats = await resp.json()
                response_text = (
                    f"📊 Статистика кампаний:\n"
                    f"Показов: {stats.get('impressions_count', 0)}\n"
                    f"Кликов: {stats.get('clicks_count', 0)}\n"
                    f"Конверсия: {stats.get('conversion', 0)}\n"
                    f"Потрачено на показы: {stats.get('spent_impressions', 0)}\n"
                    f"Потрачено на клики: {stats.get('spent_clicks', 0)}\n"
                    f"Общий расход: {stats.get('spent_total', 0)}\n"
                )
                await message.answer(response_text)
            else:
                await message.answer("Ошибка при получении статистики. Попробуйте позже.")


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
