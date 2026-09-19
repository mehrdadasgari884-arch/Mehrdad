import os
import json
import logging
from pathlib import Path
from dotenv import load_dotenv

from instagrapi import Client
from instagrapi.exceptions import LoginRequired, ChallengeRequired, FeedbackRequired

from .utils import logger, human_delay, persian_log
from .database import BotDB

load_dotenv()

class SetyBot:
    """
    Instagram Bot dedicated to @Sety_bozorg
    Handles login, session persistence, and core client
    """
    def __init__(self, config_path="config.example.json"):
        self.config = self.load_config(config_path)
        self.db = BotDB()
        
        self.username = os.getenv("IG_USERNAME")
        self.password = os.getenv("IG_PASSWORD")
        self.target_username = os.getenv("TARGET_USERNAME", "Sety_bozorg")
        self.session_file = os.getenv("SESSION_FILE", "sessions/session.json")
        self.proxy = os.getenv("IG_PROXY")

        self.client = Client()
        self.client.delay_range = [1, 3]
        
        if self.proxy:
            self.client.set_proxy(self.proxy)
            logger.info(f"🌐 Proxy set: {self.proxy}")

        # Ensure session dir exists
        Path(self.session_file).parent.mkdir(parents=True, exist_ok=True)

    def load_config(self, path):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            logger.warning(f"Config {path} not found, using defaults")
            return {
                "safety": {"min_delay_seconds": 25, "max_delay_seconds": 90},
                "comments": {"templates": ["🔥", "❤️ عالیه"]},
                "growth": {"hashtags": ["iran"]}
            }

    def login(self):
        """Smart login with session reuse"""
        if not self.username or not self.password:
            raise ValueError("IG_USERNAME and IG_PASSWORD must be set in .env")

        # Try to load session
        if os.path.exists(self.session_file):
            try:
                logger.info(f"🔑 Loading session from {self.session_file}")
                self.client.load_settings(self.session_file)
                self.client.login(self.username, self.password)
                # Validate
                self.client.get_timeline_feed()
                persian_log(f"Logged in as @{self.username} (session reused) ✅")
                return True
            except (LoginRequired, Exception) as e:
                logger.warning(f"Session invalid: {e}, fresh login...")

        try:
            logger.info(f"🔐 Logging in as {self.username}...")
            self.client.login(self.username, self.password)
            self.client.dump_settings(self.session_file)
            persian_log(f"Logged in as @{self.username} ✅ Session saved")
            return True
        except ChallengeRequired as e:
            logger.error(f"🚨 Challenge required! Check your email/SMS: {e}")
            logger.info("Please approve login from your Instagram app, then rerun")
            return False
        except FeedbackRequired as e:
            logger.error(f"🚨 Instagram feedback / rate limit: {e}")
            return False
        except Exception as e:
            logger.error(f"❌ Login failed: {e}")
            return False

    def get_target_info(self):
        """Get info about @Sety_bozorg"""
        try:
            user_info = self.client.user_info_by_username(self.target_username)
            self.db.save_target_stats(
                self.target_username,
                user_info.follower_count,
                user_info.following_count,
                user_info.media_count
            )
            logger.info(f"📊 @{self.target_username}: {user_info.follower_count} followers, {user_info.media_count} posts")
            return user_info
        except Exception as e:
            logger.error(f"Failed to get target info: {e}")
            return None

    def get_target_followers(self, amount=100):
        """Get followers of @Sety_bozorg for interaction"""
        try:
            user_id = self.client.user_id_from_username(self.target_username)
            followers = self.client.user_followers(user_id, amount=amount)
            logger.info(f"👥 Got {len(followers)} followers of @{self.target_username}")
            return followers
        except Exception as e:
            logger.error(f"Failed to get followers: {e}")
            return {}

    def get_target_medias(self, amount=12):
        try:
            user_id = self.client.user_id_from_username(self.target_username)
            medias = self.client.user_medias(user_id, amount=amount)
            logger.info(f"📸 Got {len(medias)} posts from @{self.target_username}")
            return medias
        except Exception as e:
            logger.error(f"Failed to get medias: {e}")
            return []

    def logout(self):
        try:
            self.client.logout()
            logger.info("Logged out")
        except:
            pass
