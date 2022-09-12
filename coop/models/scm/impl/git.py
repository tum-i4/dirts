# Copyright (c) 2019-present. The coop authors.
import os
import re
from pathlib import Path
from typing import Union, Optional, Dict, List

from git import Repo

from ..base import (
    SCMClient,
    Changelist,
    Repository,
    ChangelistItemAction,
    ChangelistItem,
    ChangelistItemKind,
    Commit,
)
from ....util.logging.logger import get_logger
from ....util.os.exec import check_executable_exists

_LOGGER = get_logger(__name__)


class GitClient(SCMClient):
    # mappings from log/show
    # https://mirrors.edge.kernel.org/pub/software/scm/git/docs/git-diff-tree.html#_raw_output_format
    action_mapping: Dict[str, ChangelistItemAction] = {
        "A": ChangelistItemAction.ADDED,
        "?": ChangelistItemAction.ADDED,
        "C": ChangelistItemAction.ADDED,  # copy of a file into a new one, followed by score
        "D": ChangelistItemAction.DELETED,
        "!": ChangelistItemAction.DELETED,
        "M": ChangelistItemAction.MODIFIED,
        "R": ChangelistItemAction.MODIFIED,  # renaming, followed by score, e.g. R095 for 95%
        "T": ChangelistItemAction.MODIFIED,  # change in the type of the file
    }

    def __init__(self, repository: Repository) -> None:
        self.repository = repository
        executable: str = "git"
        if check_executable_exists(executable) is None:
            raise Exception("Missing executable {}.".format(executable))
        self.git_repo: Repo = Repo(repository.path)

    @classmethod
    def create_client(cls, path: str) -> "GitClient":
        repository: Repository = Repository(path=path, repository_type="git")
        return cls(repository=repository)

    def get_repository(self) -> Repository:
        return self.repository

    def get_file_content_at_commit(
            self, revision: Union[int, str], file_path: Path
    ) -> str:
        valid_file_path: str = str(
            file_path.relative_to(Path(self.repository.path).absolute())
            if file_path.is_absolute()
            else file_path
        ).replace(os.sep, "/")
        git_object: str = f"{revision}:{valid_file_path}"
        content: str = self.git_repo.git.show(git_object)
        return content

    def get_diff(
            self,
            from_revision: Union[int, str],
            to_revision: Optional[Union[int, str]] = "HEAD",
    ) -> Changelist:
        return self._get_changelist_from_objects(
            objects="{}..{}".format(from_revision, to_revision)
        )

    def get_status(self) -> Changelist:
        changes: List[List[str]] = self._changes_from_git_status()
        return self._parse_changelist_from_changes(changes=changes)

    def reset_repo(self, rm_dirs: bool = True):
        self.git_repo.git.reset(hard=True)
        self.git_repo.git.clean(force=True, d=rm_dirs)

    def get_commit_from_repo(self, commit_id: Optional[str] = None, search_terms: Optional[list[str]] = None) -> Optional[
        Commit]:
        # get by id or current HEAD if None
        try:
            commit = self.git_repo.commit(rev=commit_id)
        except Exception as e:
            _LOGGER.debug("Could not find commit {}.".format(commit_id))
            _LOGGER.debug(e)
            return None
        # convert to domain object
        commit = Commit(
            commit_str=commit.hexsha,
            author=commit.author.name,
            message=commit.message,
            timestamp=commit.committed_datetime,
            changelist=self.get_changelist_from_commit(commit.hexsha, search_terms=search_terms),
            repo=self.repository,
        )
        return commit

    def get_changelist_from_commit(self, commit_sha: str, search_terms: Optional[list[str]] = None) -> Changelist:
        return self._get_changelist_from_objects(objects=commit_sha, search_terms=search_terms)

    def get_parent_commit(self, commit_sha: str) -> str:
        parent = self.git_repo.git.rev_parse(commit_sha + "^")
        return parent

    def _get_changelist_from_objects(self, objects: str, search_terms: Optional[list[str]] = None) -> Changelist:
        changes: List[List[str]] = self._changes_from_git_show(objects=objects)
        if objects.__contains__(".."):
            return self._parse_changelist_from_changes(changes=changes)
        else:
            arg = objects
            if objects not in self.git_repo.git.rev_list(objects, max_parents=0).splitlines():
                arg += "^.." + objects
            return self._parse_changelist_from_changes(changes=self._add_changes_content_from_git_diff(changes, arg),
                                                       search_terms=search_terms)

    def _changes_from_git_show(self, objects: str) -> List[List[str]]:
        return [change for change in self._parse_raw_changes(
            self.git_repo.git.show(
                objects,
                pretty="format:",
                oneline=True,
                name_status=True,
                m="--first-parent",  # this will use the first parent commit in a merge commit
            )) if len(change) > 1]

    def _changes_from_git_status(self) -> List[List[str]]:
        return self._parse_raw_changes(self.git_repo.git.status(porcelain=True))

    def _parse_raw_changes_content(self, raw_changes: str) -> dict[str]:
        per_files = raw_changes.split("diff --git")
        changes: dict[str] = dict()

        for file in per_files[1:]:
            file_path = None

            if (file.__contains__("@@")):
                [header, change] = file.split("@@", maxsplit=1)
                file_matcher = re.search(" b\\/(.*)", header)
                if file_matcher:
                    file_path = file_matcher.group(1)
                change = "@@" + change
            else:
                change = None

            changes[file_path] = change

        return changes

    def _add_changes_content_from_git_diff(self, changes: list[list[str]], objects: str) -> list[list[str]]:
        result = self.git_repo.git.diff(objects, unified=0, text=True)
        changes_contents = self._parse_raw_changes_content(result)
        for change in changes:
            path = change[1]
            value = changes_contents[path] if path in changes_contents else ""
            change.append(value)

        return changes

    @classmethod
    def _parse_raw_changes(cls, raw_changes: str) -> List[List[str]]:
        changes: List[List[str]] = list(
            map(
                lambda c: c.split(),
                raw_changes.splitlines(),
            )
        )
        return changes

    @classmethod
    def _parse_changelist_from_changes(cls, changes: List[List[str]],
                                       search_terms: Optional[list[str]] = None) -> Changelist:
        return Changelist(
            items=[
                ChangelistItem(
                    action=cls._get_changelist_item_action(change[0]),
                    filepath=change[1],
                    kind=ChangelistItemKind.FILE,
                    content=(change[2].encode('utf-8', 'replace').decode('utf-8').replace("\x00", "") if len(change) > 2 else None),
                    relevant=any(change[2].encode('utf-8', 'replace').decode('utf-8').replace("\x00", "").__contains__(term) for term in
                                 search_terms) if search_terms and len(change) > 2 else False,
                )
                for change in changes
                if len(change) > 0 and len(change[0]) <= 2
            ]
        )

    @classmethod
    def _get_changelist_item_action(cls, status_chars: str) -> ChangelistItemAction:
        # set default
        action: ChangelistItemAction = ChangelistItemAction.MODIFIED
        # get status from first char
        if len(status_chars) > 0:
            action = cls.action_mapping[status_chars[0]]
        return action
