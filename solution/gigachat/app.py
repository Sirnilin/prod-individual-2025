from flask import Flask, request, jsonify
from langchain_gigachat.chat_models import GigaChat
from langchain_core.messages import HumanMessage, SystemMessage
import os

app = Flask(__name__)

credentials = os.getenv("GIGACHAT_CREDENTIALS", "ваш_ключ_авторизации")

model = GigaChat(
    credentials=credentials,
    verify_ssl_certs=False,
    scope="GIGACHAT_API_PERS",
    model="GigaChat"
)


@app.route('/generate_ad', methods=['POST'])
def generate_ad():
    data = request.get_json()
    if not data or 'campaign_description' not in data:
        return jsonify({'error': 'Параметр campaign_description обязателен'}), 400

    campaign_description = data['campaign_description']

    system_prompt = (
        "Ты талантливый копирайтер, специализирующийся на создании эффективных рекламных текстов. "
        "На основе описания кампании напиши креативный рекламный текст, который привлечёт внимание аудитории."
    )

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=campaign_description)
    ]

    try:
        response = model.invoke(messages)
        ad_text = response.content
        return jsonify({'ad_text': ad_text})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
