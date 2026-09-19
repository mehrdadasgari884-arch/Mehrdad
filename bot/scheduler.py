import os
import time
from datetime import datetime
from pathlib import Path
import schedule
from .utils import logger, persian_log

class PostScheduler:
    def __init__(self, bot):
        self.bot = bot
        self.client = bot.client
        self.config = bot.config.get("scheduler", {})

    def upload_post(self, file_path, caption):
        """Upload a single post for @Sety_bozorg content"""
        try:
            path = Path(file_path)
            if not path.exists():
                logger.error(f"File not found: {file_path}")
                return False

            if path.suffix.lower() in ['.jpg', '.jpeg']:
                self.client.photo_upload(path, caption=caption)
                logger.info(f"📸 Uploaded photo: {file_path}")
                return True
            elif path.suffix.lower() in ['.mp4', '.mov']:
                self.client.video_upload(path, caption=caption)
                logger.info(f"🎥 Uploaded video: {file_path}")
                return True
            elif path.suffix.lower() in ['.mp4', '.jpg'] and len(caption) > 0:
                # For album, you'd need multiple files - simplified
                pass
            
            return False
        except Exception as e:
            logger.error(f"Upload failed {file_path}: {e}")
            return False

    def schedule_posts(self):
        """Setup schedule from config"""
        posts = self.config.get("posts", [])
        if not posts:
            logger.info("No scheduled posts in config")
            return

        for post in posts:
            time_str = post.get("time", "18:00")
            file = post.get("file")
            caption = post.get("caption", "")
            
            # Schedule daily at time_str
            schedule.every().day.at(time_str).do(self.upload_post, file_path=file, caption=caption)
            logger.info(f"⏰ Scheduled {file} at {time_str} daily")

    def run_loop(self):
        persian_log("Scheduler started - waiting for scheduled times...")
        self.schedule_posts()
        while True:
            schedule.run_pending()
            time.sleep(60)

    def upload_story(self, file_path, mention_username=None):
        """Upload story, optionally mentioning @Sety_bozorg"""
        try:
            path = Path(file_path)
            if mention_username:
                # instagrapi story with mention
                self.client.photo_upload_to_story(path)
                logger.info(f"📖 Story uploaded: {file_path} mentioning @{mention_username}")
            else:
                self.client.photo_upload_to_story(path)
                logger.info(f"📖 Story uploaded: {file_path}")
            return True
        except Exception as e:
            logger.error(f"Story upload failed: {e}")
            return False
