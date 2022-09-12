# Copyright (c) 2019-present. The coop authors.
import os
import subprocess
from contextlib import contextmanager
from typing import Generator, Optional

from git import Repo

from ..logging.logger import get_logger
from ..os.exec import check_executable_exists
from ..os.temp import temp_path
from ...models.scm.base import Repository

_LOGGER = get_logger(__name__)


def is_git_repo(path: str) -> bool:
    result: bool = False
    executable: str = "git"

    if not check_executable_exists(executable):
        _LOGGER.debug("Could not find executable `{}`.".format(executable))
        return result

    return (
        subprocess.call(
            [executable, "-C", path, "status"],
            stderr=subprocess.STDOUT,
            stdout=open(os.devnull, "w"),
        )
        == 0
    )


@contextmanager
def clone_repo_if_not_exists(path: str) -> Generator[Repo, None, None]:
    """
    Will create a temporary clone of a git repository if the provided path is not local.
    Caller will need to take care of removing the directory again.

    Example:

    ```
    repo = clone_repo_if_not_exists(path)
    shutil.rmtree(repo.working_dir)
    ```

    :param path: URL or FS path
    :return:
    """
    git_repo = Repo(path)
    if not git_repo.bare:
        yield git_repo

    with temp_clone(repo_path=path) as (repo_path, _):
        yield Repo(repo_path)


@contextmanager
def temp_repo(mkdir: bool = False):
    """
    Initialize a repository in the current path (i.e., the git remote).
    """
    with temp_path() as repo_path:
        repo: Repo = Repo.init(path=repo_path, mkdir=mkdir, bare=True)

        repository: Repository = Repository(path=repo_path, repository_type="git")

        yield repo_path, repository


@contextmanager
def temp_clone(repo_path: Optional[str] = None):
    """
    Clones a repository in the current path to a temporary location.

    :param repo_path:
    """
    path: str = repo_path if repo_path else os.getcwd()

    with temp_path() as working_path:
        repo: Repo = Repo.clone_from(url=path, to_path=working_path)

        repository: Repository = Repository(path=working_path, repository_type="git")

        yield working_path, repository
