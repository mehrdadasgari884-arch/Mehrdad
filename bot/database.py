import sqlite3
import os
from datetime import datetime

DB_PATH = "bot_data.db"

class BotDB:
    def __init__(self, path=DB_PATH):
        self.path = path
        self.init_db()

    def init_db(self):
        conn = sqlite3.connect(self.path)
        c = conn.cursor()
        c.execute("""
        CREATE TABLE IF NOT EXISTS actions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            action_type TEXT,
            target_username TEXT,
            target_user_id TEXT,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            success BOOLEAN
        )
        """)
        c.execute("""
        CREATE TABLE IF NOT EXISTS users_interacted (
            user_id TEXT PRIMARY KEY,
            username TEXT,
            first_interaction DATETIME,
            last_interaction DATETIME,
            follow_status TEXT,
            like_count INTEGER DEFAULT 0,
            is_follower_of_target BOOLEAN DEFAULT 0
        )
        """)
        c.execute("""
        CREATE TABLE IF NOT EXISTS target_stats (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            target_username TEXT,
            followers INTEGER,
            following INTEGER,
            posts INTEGER,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
        )
        """)
        conn.commit()
        conn.close()

    def log_action(self, action_type, target_username, target_user_id, success=True):
        conn = sqlite3.connect(self.path)
        c = conn.cursor()
        c.execute("INSERT INTO actions (action_type, target_username, target_user_id, success) VALUES (?,?,?,?)",
                  (action_type, target_username, target_user_id, success))
        conn.commit()
        conn.close()

    def add_user(self, user_id, username, is_follower_of_target=False):
        conn = sqlite3.connect(self.path)
        c = conn.cursor()
        now = datetime.now().isoformat()
        c.execute("""
        INSERT OR IGNORE INTO users_interacted (user_id, username, first_interaction, last_interaction, is_follower_of_target)
        VALUES (?,?,?,?,?)
        """, (user_id, username, now, now, is_follower_of_target))
        c.execute("UPDATE users_interacted SET last_interaction=?, username=? WHERE user_id=?",
                  (now, username, user_id))
        conn.commit()
        conn.close()

    def save_target_stats(self, target_username, followers, following, posts):
        conn = sqlite3.connect(self.path)
        c = conn.cursor()
        c.execute("INSERT INTO target_stats (target_username, followers, following, posts) VALUES (?,?,?,?)",
                  (target_username, followers, following, posts))
        conn.commit()
        conn.close()

    def get_stats(self):
        conn = sqlite3.connect(self.path)
        c = conn.cursor()
        c.execute("SELECT action_type, COUNT(*) FROM actions GROUP BY action_type")
        actions = dict(c.fetchall())
        c.execute("SELECT COUNT(*) FROM users_interacted")
        total_users = c.fetchone()[0]
        c.execute("SELECT target_username, followers, timestamp FROM target_stats ORDER BY timestamp DESC LIMIT 10")
        target_history = c.fetchall()
        conn.close()
        return {
            "actions": actions,
            "total_users": total_users,
            "target_history": target_history
        }
