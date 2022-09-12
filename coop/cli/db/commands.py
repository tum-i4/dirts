# Copyright (c) 2019-present. The coop authors.
import os
import subprocess

import click

from ...db.base import DBConnection
from ...util.logging.cli import (
    click_echo_success,
    click_echo_failure,
    start_spinner,
    click_echo_info,
)
from ...util.logging.logger import get_logger
from ...util.os.exec import check_executable_exists

_LOGGER = get_logger(__name__)

# connect
SUCCESS_CONNECTED_MSG = "Connection successfully established."
FAILED_CONNECTED_MSG = "Failed to connect to database."
# dump
SUCCESS_DUMP_MSG = "Successfully created database dump."
FAILED_DUMP_MSG = "Failed to create database dump."
# restore
SUCCESS_RESTORE_MSG = "Successfully restored database."
FAILED_RESTORE_MSG = "Failed to restore database."
# migrate
SUCCESS_MIGRATE_MSG = "Successfully migrated database schema."
FAILED_MIGRATE_MSG = "Failed to migrate database schema."
# drop
SUCCESS_DROP_MSG = "Successfully dropped database schema."
FAILED_DROP_MSG = "Failed to drop database schema."


@click.group(name="db")
@click.argument("url", type=str)
@click.pass_context
def db(ctx, url: str):
    """
    Interact with databases.

    Arguments:

        URL is the database connection string of the format dialect[+driver]://user:password@host/dbname[?key=value..].

    Examples:

        $ coop db postgresql://user:pass@localhost:5432/db ping
    """
    # set options
    echo = "debug" if ctx.obj["debug"] else False

    # create db connection
    try:
        spinner = start_spinner("Connecting to database {}".format(url))
        conn = DBConnection(url, echo=echo)
        spinner.stop()
        click_echo_success(SUCCESS_CONNECTED_MSG)
        ctx.obj["connection"] = conn
    except Exception as e:
        _LOGGER.debug(e)
        click_echo_failure(FAILED_CONNECTED_MSG)
        raise e


@db.command(name="ping")
@click.pass_obj
def ping(ctx):
    """
    Ping database connection.
    """
    conn: DBConnection = ctx["connection"]
    co = conn.engine.connect()
    click_echo_info(
        "Connection is currently {}.".format(
            ("busy" if co.in_transaction() else "idle")
        )
    )
    co.close()


@db.command(name="migrate")
@click.pass_obj
def migrate(ctx):
    """
    Migrate database schema.

    Examples:

        $ coop db postgresql://user:pass@localhost:5432/db migrate
    """
    conn: DBConnection = ctx["connection"]
    try:
        spinner = start_spinner("Trying to migrate schema...")
        conn.create_schema()
        spinner.stop()
        click_echo_success(SUCCESS_MIGRATE_MSG)
    except Exception as e:
        _LOGGER.debug(e)
        click_echo_failure(FAILED_MIGRATE_MSG)
        raise e


@db.command(name="drop")
@click.pass_obj
def drop(ctx):
    """
    Drop database schema.

    Examples:

        $ coop db postgresql://user:pass@localhost:5432/db drop
    """
    conn: DBConnection = ctx["connection"]
    try:
        spinner = start_spinner("Trying to drop schema...")
        conn.delete_schema()
        spinner.stop()
        click_echo_success(SUCCESS_DROP_MSG)
    except Exception as e:
        _LOGGER.debug(e)
        click_echo_failure(FAILED_DROP_MSG)
        raise e


@db.command(name="dump")
@click.argument("output", type=click.Path(exists=False))
@click.pass_obj
def dump(ctx, output):
    """
    Create database dump, e.g., for backing up data.

    Arguments:

        OUTPUT filepath to dump file.

    Examples:

        $ coop db postgresql://user:pass@localhost:5432/db dump backup.dump
    """
    conn: DBConnection = ctx["connection"]
    try:
        executable: str = "pg_dump"
        # check for executables
        if check_executable_exists(executable) is None:
            raise Exception("Missing executable {}.".format(executable))

        # input validation
        if os.path.isdir(output):
            raise Exception("No filepath provided.")

        if ".dump" not in output:
            output += ".backup.dump"

        # create command
        cmd = executable
        if ctx["debug"]:
            cmd += " --verbose"
        cmd += " -Z 9 --format=custom --no-owner -f {} {}".format(output, conn.url)

        # execute command
        spinner = start_spinner("Trying to dump database...")
        proc = subprocess.Popen(cmd, shell=True)
        exit_code = proc.wait()
        if exit_code != 0:
            raise Exception("Failed to execute {}.".format(executable))
        spinner.stop()
        click_echo_success(SUCCESS_DUMP_MSG)
    except Exception as e:
        _LOGGER.debug(e)
        click_echo_failure(FAILED_DUMP_MSG)
        raise e


@db.command(name="restore")
@click.argument("backup", type=click.Path(exists=True))
@click.option(
    "--clean", is_flag=True, default=False, help="Clean database before restoring."
)
@click.pass_obj
def restore(ctx, backup, clean):
    """
    Restore database from dump, e.g., from backup.

    Arguments:

        BACKUP filepath to dump file.

    Examples:

        $ coop db postgresql://user:pass@localhost:5432/db restore backup.dump
    """
    conn: DBConnection = ctx["connection"]
    try:
        executable: str = "pg_restore"
        # check for executables
        if check_executable_exists(executable) is None:
            raise Exception("Missing executable {}.".format(executable))

        # input validation
        if ".dump" not in backup:
            raise Exception("Wrong file format.")

        # create command
        cmd = executable
        if clean:
            cmd += " --clean"
        if ctx["debug"]:
            cmd += " --verbose"
        cmd += " -d {} {}".format(conn.url, backup)

        # execute command
        spinner = start_spinner("Trying to restore database...")
        proc = subprocess.Popen(cmd, shell=True)
        exit_code = proc.wait()
        _LOGGER.info(exit_code)
        if exit_code != 0:
            raise Exception("Failed to execute {}.".format(executable))
        spinner.stop()
        click_echo_success(SUCCESS_RESTORE_MSG)
    except Exception as e:
        _LOGGER.debug(e)
        click_echo_failure(FAILED_RESTORE_MSG)
        raise e
