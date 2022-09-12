# Copyright (c) 2019-present. The coop authors.

import logging

import click

from .db.commands import db
from .. import __version__
from ..util.logging.logger import configure_logging_verbosity

# debug
DEBUG_MODE_MSG = "Debug mode is on."

# status
STATUS_MSG = "Running version {} of coop.".format(__version__)

# input validation
INVALID_PARAMETERS_MSG = "Invalid parameters provided."


@click.group(name="coop")
@click.pass_context
@click.option("--debug", is_flag=True, default=False, help="Show debug information.")
def entry_point(ctx, debug):
    """
    coop CLI
    """
    ctx.ensure_object(dict)
    ctx.obj["debug"] = debug

    # set logging level
    if debug:
        configure_logging_verbosity(verbosity=logging.DEBUG)
        click.echo(click.style(DEBUG_MODE_MSG, fg="red", bold=True))
    else:
        configure_logging_verbosity(verbosity=logging.INFO)


@entry_point.command()
@click.pass_obj
def version(ctx):
    """
    Get coop version.
    """
    click.echo(click.style(STATUS_MSG, fg="green", bold=True))


entry_point.add_command(db)

