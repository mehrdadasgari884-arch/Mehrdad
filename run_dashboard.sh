#!/bin/bash
# Quick start dashboard for @Sety_bozorg bot
echo "🤖 Starting @Sety_bozorg Bot Dashboard..."
echo "Target: @Sety_bozorg"
echo ""

if [ ! -f .env ]; then
    echo "⚠️  .env not found, creating from example..."
    cp .env.example .env
    echo "📝 Please edit .env with your IG credentials"
    echo "   nano .env"
    exit 1
fi

pip install -q -r requirements.txt

echo "🚀 Launching on 0.0.0.0:8000"
python main.py dashboard --port 8000
