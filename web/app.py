from fastapi import FastAPI, Request, Form, Depends, HTTPException
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
import os
import json
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="Sety_bozorg Bot Dashboard", description="Dashboard for @Sety_bozorg Instagram Bot")

templates = Jinja2Templates(directory="web/templates")

# Simple auth
DASHBOARD_PASSWORD = os.getenv("DASHBOARD_PASSWORD", "admin123")

def get_bot_instance():
    from bot.instagram_bot import SetyBot
    bot = SetyBot()
    return bot

@app.get("/", response_class=HTMLResponse)
async def dashboard(request: Request):
    from bot.database import BotDB
    db = BotDB()
    stats = db.get_stats()
    
    # Try to get target info from latest DB entry
    target_username = os.getenv("TARGET_USERNAME", "Sety_bozorg")
    
    # Load config
    config_path = "config.example.json"
    config = {}
    if Path(config_path).exists():
        with open(config_path, 'r', encoding='utf-8') as f:
            config = json.load(f)

    return templates.TemplateResponse("dashboard.html", {
        "request": request,
        "stats": stats,
        "target_username": target_username,
        "config": config,
        "bot_username": os.getenv("IG_USERNAME", "Not set")
    })

@app.get("/api/stats")
async def api_stats():
    from bot.database import BotDB
    db = BotDB()
    return db.get_stats()

@app.get("/api/analyze/{username}")
async def api_analyze(username: str):
    from bot.instagram_bot import SetyBot
    from bot.actions import BotActions
    
    bot = SetyBot()
    # Override target for this request
    bot.target_username = username
    if not bot.login():
        raise HTTPException(status_code=401, detail="Login failed, check .env")
    
    actions = BotActions(bot)
    analysis = actions.analyze_target()
    if not analysis:
        raise HTTPException(status_code=404, detail="Analysis failed")
    return analysis

@app.post("/api/action/interact")
async def api_interact(amount: int = 20):
    from bot.instagram_bot import SetyBot
    from bot.actions import BotActions
    bot = SetyBot()
    if not bot.login():
        raise HTTPException(status_code=401, detail="Login failed")
    actions = BotActions(bot)
    total = actions.interact_with_followers_of_target(amount=amount)
    return {"interacted": total, "target": bot.target_username}

@app.get("/health")
async def health():
    return {"status": "ok", "target": os.getenv("TARGET_USERNAME", "Sety_bozorg"), "service": "Sety_bozorg Bot"}

# Mount static if exists
if Path("web/static").exists():
    app.mount("/static", StaticFiles(directory="web/static"), name="static")
