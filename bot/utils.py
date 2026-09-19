import random
import time
import logging
from datetime import datetime
from rich.console import Console
from rich.logging import RichHandler

console = Console()

def setup_logger(name="sety_bot"):
    logging.basicConfig(
        level=logging.INFO,
        format="%(message)s",
        datefmt="[%X]",
        handlers=[RichHandler(console=console, rich_tracebacks=True)]
    )
    return logging.getLogger(name)

logger = setup_logger()

def human_delay(min_sec=25, max_sec=90):
    """Sleep random time like a human"""
    delay = random.uniform(min_sec, max_sec)
    logger.info(f"⏳ Sleeping {delay:.1f}s...")
    time.sleep(delay)

def random_comment(templates):
    return random.choice(templates)

def is_safe_to_interact(user_info, config):
    """Check safety filters"""
    safety = config.get("safety", {})
    followers = getattr(user_info, 'follower_count', 0) or 0
    
    min_f = safety.get("min_followers_to_interact", 0)
    max_f = safety.get("max_followers_to_interact", 9999999)
    
    if followers < min_f or followers > max_f:
        return False, f"followers {followers} out of range [{min_f}-{max_f}]"
    
    if safety.get("dont_interact_with_private") and getattr(user_info, 'is_private', False):
        return False, "private account"
    
    if safety.get("dont_interact_with_verified") and getattr(user_info, 'is_verified', False):
        return False, "verified account"
    
    return True, "ok"

def format_number(n):
    if n >= 1_000_000:
        return f"{n/1_000_000:.1f}M"
    if n >= 1000:
        return f"{n/1000:.1f}K"
    return str(n)

def persian_log(msg):
    console.print(f"[bold cyan][@Sety_bozorg][/] {msg}")

def get_greeting():
    hour = datetime.now().hour
    if 5 <= hour < 12:
        return "صبح بخیر"
    elif 12 <= hour < 18:
        return "عصر بخیر"
    else:
        return "شب بخیر"
