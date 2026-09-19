#!/usr/bin/env python3
"""
@Sety_bozorg Instagram Bot
Main CLI - Mehrdad Project

Usage:
  python main.py login              - Test login
  python main.py analyze            - Analyze @Sety_bozorg profile
  python main.py interact --amount 50  - Interact with followers of @Sety_bozorg
  python main.py hashtags --tags iran,tehran --amount 20
  python main.py dashboard          - Start web dashboard
  python main.py stats              - Show bot stats
"""

import argparse
import json
import os
from bot.instagram_bot import SetyBot
from bot.actions import BotActions
from bot.utils import logger, persian_log, console
from rich.table import Table
from rich.panel import Panel

def cmd_login(args):
    bot = SetyBot(config_path=args.config)
    if bot.login():
        console.print(Panel.fit(f"✅ Logged in as @{bot.username}\nTarget: @{bot.target_username}", title="Login Success", border_style="green"))
        info = bot.get_target_info()
    else:
        console.print("[red]Login failed[/red]")

def cmd_analyze(args):
    bot = SetyBot(config_path=args.config)
    if not bot.login():
        return
    actions = BotActions(bot)
    analysis = actions.analyze_target()
    if not analysis:
        return
    
    table = Table(title=f"Analysis: @{analysis['username']}", show_header=True)
    table.add_column("Metric", style="cyan")
    table.add_column("Value", style="magenta")
    table.add_row("Full Name", analysis['full_name'] or "-")
    table.add_row("Followers", f"{analysis['followers']:,}")
    table.add_row("Following", f"{analysis['following']:,}")
    table.add_row("Posts", str(analysis['posts']))
    table.add_row("Avg Likes", f"{analysis['avg_likes']:.0f}")
    table.add_row("Avg Comments", f"{analysis['avg_comments']:.0f}")
    table.add_row("Engagement Rate", f"{analysis['engagement_rate']:.2f}%")
    table.add_row("Private", str(analysis['is_private']))
    table.add_row("Verified", str(analysis['is_verified']))
    console.print(table)

    # Save to file
    with open(f"analysis_{analysis['username']}.json", "w", encoding="utf-8") as f:
        json.dump(analysis, f, ensure_ascii=False, indent=2)
    console.print(f"💾 Saved to analysis_{analysis['username']}.json")

def cmd_interact(args):
    bot = SetyBot(config_path=args.config)
    if not bot.login():
        return
    actions = BotActions(bot)
    persian_log(f"Starting community interaction for @{bot.target_username}")
    total = actions.interact_with_followers_of_target(amount=args.amount)
    console.print(f"[green]Done! Interacted with {total} users[/green]")

def cmd_hashtags(args):
    bot = SetyBot(config_path=args.config)
    if not bot.login():
        return
    actions = BotActions(bot)
    tags = args.tags.split(",") if args.tags else None
    total = actions.like_by_hashtags(hashtags=tags, amount_per_tag=args.amount)
    console.print(f"[green]Liked {total} posts by hashtags[/green]")

def cmd_stats(args):
    from bot.database import BotDB
    db = BotDB()
    stats = db.get_stats()
    
    table = Table(title="Bot Stats")
    table.add_column("Action", style="cyan")
    table.add_column("Count", style="green")
    for action, count in stats['actions'].items():
        table.add_row(action, str(count))
    console.print(table)
    console.print(f"Total unique users interacted: [bold]{stats['total_users']}[/bold]")
    
    if stats['target_history']:
        console.print("\n[bold]Target growth history:[/bold]")
        for username, followers, ts in stats['target_history'][:5]:
            console.print(f"  {ts} - @{username}: {followers} followers")

def cmd_dashboard(args):
    import uvicorn
    console.print(f"[green]Starting dashboard on http://0.0.0.0:{args.port}[/green]")
    uvicorn.run("web.app:app", host="0.0.0.0", port=args.port, reload=False)

def main():
    parser = argparse.ArgumentParser(description="Instagram Bot for @Sety_bozorg - Mehrdad")
    parser.add_argument("--config", default="config.example.json", help="Config file path")
    sub = parser.add_subparsers(dest="command", required=True)

    p_login = sub.add_parser("login", help="Test login")
    p_login.set_defaults(func=cmd_login)

    p_analyze = sub.add_parser("analyze", help="Analyze @Sety_bozorg")
    p_analyze.set_defaults(func=cmd_analyze)

    p_interact = sub.add_parser("interact", help="Interact with followers of target")
    p_interact.add_argument("--amount", type=int, default=50, help="Number of users to interact with")
    p_interact.set_defaults(func=cmd_interact)

    p_hash = sub.add_parser("hashtags", help="Like by hashtags")
    p_hash.add_argument("--tags", type=str, default="", help="Comma separated hashtags")
    p_hash.add_argument("--amount", type=int, default=10, help="Amount per tag")
    p_hash.set_defaults(func=cmd_hashtags)

    p_stats = sub.add_parser("stats", help="Show stats")
    p_stats.set_defaults(func=cmd_stats)

    p_dash = sub.add_parser("dashboard", help="Start web dashboard")
    p_dash.add_argument("--port", type=int, default=int(os.getenv("DASHBOARD_PORT", 8000)))
    p_dash.set_defaults(func=cmd_dashboard)

    args = parser.parse_args()
    args.func(args)

if __name__ == "__main__":
    main()
