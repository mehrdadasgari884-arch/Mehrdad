import random
import os
from .utils import logger, human_delay, is_safe_to_interact, random_comment, persian_log

class BotActions:
    def __init__(self, bot):
        self.bot = bot
        self.client = bot.client
        self.config = bot.config
        self.db = bot.db

        self.like_enabled = os.getenv("LIKE_ENABLED", "true").lower() == "true"
        self.follow_enabled = os.getenv("FOLLOW_ENABLED", "true").lower() == "true"
        self.comment_enabled = os.getenv("COMMENT_ENABLED", "false").lower() == "true"
        self.dm_enabled = os.getenv("DM_ENABLED", "false").lower() == "true"
        self.story_enabled = os.getenv("STORY_VIEW_ENABLED", "true").lower() == "true"

        self.hourly_limits = {
            "like": int(os.getenv("HOURLY_LIKE_LIMIT", 30)),
            "follow": int(os.getenv("HOURLY_FOLLOW_LIMIT", 15)),
            "comment": int(os.getenv("HOURLY_COMMENT_LIMIT", 10)),
        }

    def interact_with_followers_of_target(self, amount=50):
        """
        Core growth strategy for @Sety_bozorg:
        Interact with people who already follow @Sety_bozorg
        to build community and increase engagement
        """
        persian_log(f"Starting interaction with followers of @{self.bot.target_username} - target {amount} users")
        
        followers = self.bot.get_target_followers(amount=amount)
        if not followers:
            return 0

        interacted = 0
        safety_cfg = self.config.get("safety", {})
        min_delay = safety_cfg.get("min_delay_seconds", 25)
        max_delay = safety_cfg.get("max_delay_seconds", 90)

        for user_id, user in list(followers.items())[:amount]:
            try:
                # Safety check
                safe, reason = is_safe_to_interact(user, self.config)
                if not safe:
                    logger.info(f"⏭️ Skipping @{user.username}: {reason}")
                    continue

                self.db.add_user(user_id, user.username, is_follower_of_target=True)

                # 1. Like their recent posts
                if self.like_enabled:
                    medias = self.client.user_medias(user_id, amount=2)
                    for media in medias[:1]:
                        try:
                            self.client.media_like(media.id)
                            self.db.log_action("like", user.username, user_id, True)
                            logger.info(f"❤️ Liked @{user.username}'s post")
                            interacted += 1
                            human_delay(2, 5)
                        except Exception as e:
                            logger.warning(f"Like failed for @{user.username}: {e}")

                # 2. Follow (occasionally)
                if self.follow_enabled and random.random() < 0.3:  # 30% follow rate
                    try:
                        self.client.user_follow(user_id)
                        self.db.log_action("follow", user.username, user_id, True)
                        logger.info(f"➕ Followed @{user.username}")
                    except Exception as e:
                        logger.warning(f"Follow failed: {e}")

                # 3. Story view
                if self.story_enabled:
                    try:
                        # view story if available
                        self.client.user_story_feed(user_id)
                        # instagrapi doesn't have direct story view, but we can mark as seen via private API
                    except:
                        pass

                # Human delay
                human_delay(min_delay, max_delay)

                if interacted % 20 == 0 and interacted > 0:
                    logger.info(f"💤 Taking a longer break after {interacted} actions...")
                    human_delay(60*5, 60*10)

            except Exception as e:
                logger.error(f"Error interacting with @{user.username}: {e}")
                human_delay(10, 20)

        persian_log(f"Finished: interacted with {interacted} users from @{self.bot.target_username}'s community")
        return interacted

    def like_by_hashtags(self, hashtags=None, amount_per_tag=10):
        """Like posts by hashtags related to @Sety_bozorg niche"""
        if hashtags is None:
            hashtags = self.config.get("growth", {}).get("hashtags", ["iran", "persian"])
        
        persian_log(f"Hashtag liking: {hashtags}")
        total_liked = 0
        
        for tag in hashtags:
            try:
                medias = self.client.hashtag_medias_recent(tag, amount=amount_per_tag)
                logger.info(f"#{tag}: found {len(medias)} posts")
                
                for media in medias:
                    try:
                        user = self.client.user_info(media.user.pk)
                        safe, reason = is_safe_to_interact(user, self.config)
                        if not safe:
                            continue
                        
                        self.client.media_like(media.id)
                        self.db.log_action("like_hashtag", user.username, str(user.pk), True)
                        total_liked += 1
                        logger.info(f"❤️ Liked #{tag} post by @{user.username}")
                        human_delay(15, 40)
                    except Exception as e:
                        logger.warning(f"Failed to like #{tag}: {e}")
                        human_delay(5, 10)
                
                human_delay(30, 60)
            except Exception as e:
                logger.error(f"Hashtag {tag} error: {e}")

        return total_liked

    def auto_comment_on_target(self, amount=5):
        """Leave supportive comments on @Sety_bozorg's own posts to boost engagement"""
        if not self.comment_enabled:
            logger.info("Comment disabled, skipping")
            return 0

        medias = self.bot.get_target_medias(amount=amount)
        templates = self.config.get("comments", {}).get("templates", ["🔥", "عالیه"])
        
        commented = 0
        for media in medias:
            try:
                # Check if we already commented
                comments = self.client.media_comments(media.id, amount=20)
                my_username = self.bot.username
                already_commented = any(c.user.username == my_username for c in comments)
                if already_commented:
                    continue

                comment_text = random_comment(templates)
                self.client.media_comment(media.id, comment_text)
                self.db.log_action("comment", self.bot.target_username, str(media.id), True)
                logger.info(f"💬 Commented on @{self.bot.target_username}: {comment_text}")
                commented += 1
                human_delay(60, 120)
            except Exception as e:
                logger.warning(f"Comment failed: {e}")

        return commented

    def analyze_target(self):
        """Deep analysis of @Sety_bozorg"""
        info = self.bot.get_target_info()
        if not info:
            return None
        
        medias = self.bot.get_target_medias(amount=12)
        total_likes = sum(m.like_count for m in medias)
        total_comments = sum(m.comment_count for m in medias)
        avg_likes = total_likes / len(medias) if medias else 0
        avg_comments = total_comments / len(medias) if medias else 0
        engagement_rate = (avg_likes + avg_comments) / info.follower_count * 100 if info.follower_count else 0

        analysis = {
            "username": info.username,
            "full_name": info.full_name,
            "followers": info.follower_count,
            "following": info.following_count,
            "posts": info.media_count,
            "is_private": info.is_private,
            "is_verified": info.is_verified,
            "avg_likes": avg_likes,
            "avg_comments": avg_comments,
            "engagement_rate": engagement_rate,
            "recent_posts": [{"likes": m.like_count, "comments": m.comment_count, "caption": (m.caption_text[:100] if m.caption_text else "")} for m in medias[:5]]
        }

        persian_log(f"📈 @{info.username} Analysis: ER {engagement_rate:.2f}% | Avg ❤️ {avg_likes:.0f} 💬 {avg_comments:.0f}")
        return analysis

    def dm_new_followers(self, welcome_text=None):
        """DM welcome to new followers (use carefully)"""
        if not self.dm_enabled:
            logger.info("DM disabled")
            return 0
        
        if not welcome_text:
            welcome_text = self.config.get("dm", {}).get("welcome_message", f"سلام! مرسی که فالو کردی 🙏 پیج @{self.bot.target_username} رو هم ببین!")

        # This would require tracking followers; simplified version
        logger.warning("DM feature is risky - Instagram heavily limits DMs. Use manually.")
        return 0
