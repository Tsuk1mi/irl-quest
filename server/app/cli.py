# Новый файл: простой CLI для разработчика (migrate, seed, runserver)
import argparse
import asyncio
import sys


def run_migrations():
    from app.migrations_runner import run_migrations as _run

    _run()


async def seed_all():
    from app.db.session import AsyncSessionLocal
    from app.seeds.users import seed_admin_user
    from app.seeds.tasks import seed_tasks

    async with AsyncSessionLocal() as session:
        await seed_admin_user()
        await seed_tasks()


def run_server(host: str = "0.0.0.0", port: int = 8000, reload: bool = False):
    import uvicorn

    uvicorn.run("server.app.main:app", host=host, port=port, reload=reload)


def main():
    parser = argparse.ArgumentParser(prog="manage.py")
    sub = parser.add_subparsers(dest="cmd")

    sub.add_parser("migrate", help="Run alembic migrations")
    sub.add_parser("seed", help="Run DB seeds (async)")
    runp = sub.add_parser("run", help="Run development server")
    runp.add_argument("--host", default="0.0.0.0")
    runp.add_argument("--port", default=8000, type=int)
    runp.add_argument("--reload", action="store_true")

    args = parser.parse_args()
    if args.cmd == "migrate":
        run_migrations()
        return
    if args.cmd == "seed":
        asyncio.run(seed_all())
        return
    if args.cmd == "run":
        run_server(host=args.host, port=args.port, reload=args.reload)
        return

    parser.print_help()


if __name__ == "__main__":
    main()

