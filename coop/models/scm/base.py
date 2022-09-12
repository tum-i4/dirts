"""
Module containing base interfaces for SCM systems.
"""
import uuid
from abc import ABC, abstractmethod
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import List, Optional, Union, Dict, Any

from ...util.logging.logger import get_logger

_LOGGER = get_logger(__name__)


class ChangelistItemAction(Enum):
    ADDED = "ADDED"
    MODIFIED = "MODIFIED"
    DELETED = "DELETED"
    NONE = "NONE"


class ChangelistItemKind(Enum):
    FILE = "FILE"
    DIR = "DIR"

class DependencyInjectionType(Enum):
    GUICE = "GUICE"
    SPRING = "SPRING"
    CDI = "CDI"

class Repository(object):
    def __init__(self, path: str, repository_type: str, di_types=None) -> None:
        super().__init__()
        if di_types is None:
            di_types = []
        self.path = path
        self.repository_type = repository_type
        self.di_types = di_types

    def __eq__(self, other: "Repository") -> bool:
        return self.path == other.path and self.repository_type == other.repository_type

    def __hash__(self) -> int:
        return hash("{}_{}".format(self.path, self.repository_type))


class ChangelistItem(object):
    def __init__(
            self,
            filepath: str,
            action: ChangelistItemAction,
            kind: ChangelistItemKind,
            relevant: bool = False,
            content: Optional[str] = None
    ) -> None:
        """
        A constructor for a ChangelistItem.

        :param filepath:
        :param action:
        :param kind:
        :param relevant:
        :param content:
        """
        self.filepath = filepath
        self.action = action
        self.kind = kind
        self.relevant = relevant
        self.content = content

    def __eq__(self, other: "ChangelistItem") -> bool:
        return self.filepath == other.filepath and self.action == other.action

    def __hash__(self) -> int:
        return hash("{}_{}".format(self.filepath, self.action))

    def __str__(self) -> str:
        return f"{self.action} {self.kind} {self.filepath}"

    def to_json(self) -> Dict:
        return {
            "filepath": self.filepath,
            "action": self.action.value,
            "kind": self.kind.value,
            "relevant": self.relevant,
            "content": self.content
        }

    @classmethod
    def from_json(cls, json: Dict) -> "ChangelistItem":
        return cls(
            filepath=json["filepath"],
            action=ChangelistItemAction(json["action"]),
            kind=ChangelistItemKind(json["kind"]),
            relevant=bool(json["relevant"]),
            content=json["content"]
        )


class Changelist(object):
    """
    Changelists are composites of change list items.
    They can comprise ChangelistItems from multiple commits or a single commit.
    Commits contain only one changelist.
    """

    def __init__(self, items: List[ChangelistItem]) -> None:
        """
        A constructor for a Changelist.

        :param items:
        """
        self.items = items

    def __eq__(self, other: "Changelist") -> bool:
        return set(self.items) == set(other.items)

    def __str__(self) -> str:
        return "\n".join([item.__str__() for item in self.items])

    def to_json(self) -> Dict:
        return {
            "items": [item.to_json() for item in self.items],
        }

    @classmethod
    def from_json(cls, json: Dict) -> "Changelist":
        return cls(
            items=[ChangelistItem.from_json(item) for item in json["items"]],
        )


class Commit(object):
    def __init__(
        self,
        commit_str: str,
        author: str,
        message: str,
        timestamp: datetime,
        changelist: Changelist,
        repo: Optional[Repository] = None,
        relevant : bool = False,
        loc_java = -1,
        files_java = -1
    ) -> None:
        self.author = author
        self.commit_str = commit_str
        self.message = message
        self.timestamp = timestamp
        self.changelist = changelist
        self.repo = repo
        self.relevant = relevant
        self.loc_java = loc_java
        self.files_java = files_java

    @classmethod
    def create_virtual_commit(
        cls, changelist: Changelist, repo: Optional[Repository] = None
    ) -> "Commit":
        now: datetime = datetime.now()
        return cls(
            commit_str="vc-{}".format(uuid.uuid4()),
            author="vc-author",
            message="vc-msg",
            timestamp=now,
            changelist=changelist,
            repo=repo,
        )

    def __eq__(self, other: "Commit") -> bool:
        return self.commit_str == other.commit_str

    def __hash__(self) -> int:
        return hash(self.commit_str)

    def __repr__(self) -> str:
        return self.commit_str

    def __lt__(self, other: "Commit") -> bool:
        return self.timestamp < other.timestamp

    def __str__(self) -> str:
        return self.commit_str

    def to_json(self) -> Dict:
        return {
            "author": self.author,
            "commit_str": self.commit_str,
            "message": self.message,
            "timestamp": self.timestamp.timestamp(),
            "changelist": self.changelist.to_json(),
        }

    @classmethod
    def from_json(cls, json: Dict) -> "Commit":
        return cls(
            author=json["author"],
            commit_str=json["commit_str"],
            message=json["message"],
            timestamp=datetime.fromtimestamp(int(json["timestamp"])),
            changelist=Changelist.from_json(json["changelist"]),
        )


class SCMClient(ABC):
    @staticmethod
    def get_client_for_path(path: str) -> Optional["SCMClient"]:
        # add imports here to prevent cyclic deps
        from ...util.scm.git import is_git_repo
        from .impl.git import GitClient

        scm_client: Optional["SCMClient"] = None
        if is_git_repo(path):
            _LOGGER.debug("Found git repository.")
            scm_client = GitClient.create_client(path=path)
        return scm_client

    @abstractmethod
    def get_repository(self) -> Repository:
        pass

    @abstractmethod
    def get_diff(
        self,
        from_revision: Union[int, str],
        to_revision: Optional[Union[int, str]] = None,
    ) -> Changelist:
        """
        Get a combined changelist depicting the diff between two revisions.
        :param from_revision:
        :param to_revision:
        :return:
        """
        pass

    @abstractmethod
    def get_file_content_at_commit(
        self, revision: Union[int, str], file_path: Path
    ) -> str:
        """
        Get the content of a file at a certain revision.

        :param revision: Revision or branch name at which to get the content of the file.
        :param file_path: File path relative to repository root.
        :return:
        """
        pass

    @abstractmethod
    def get_status(
        self,
    ) -> Changelist:
        """
        Get a changelist that contains all currently changed/added/deleted files.
        :return:
        """
        pass

    @classmethod
    def parse_commits_from_jenkins_log(cls, changeset: Dict[str, Any]) -> List[Commit]:
        """
        Parser should work with `hudson.plugins.git.GitChangeSet` and `hudson.scm.SubversionChangeLogSet`.
        :param changeset:
        :return:
        """
        commits = []
        for log_entry in changeset["items"]:
            changelist_items = []
            # TODO: this should be cleaned up with actually using the XML schema or the Java classes for checks
            # this is added here as there are two schemas:
            # `hudson.scm.SubversionChangeLogSet$LogEntry` (with `paths`) and `de.ivu.mb.ChangeSet` (without `paths`)
            paths_key = "paths" if "paths" in log_entry else "affectedPaths"
            for path in log_entry[paths_key]:
                action = ChangelistItemAction.MODIFIED
                # can only do this if there is an `editType` property (not the case in `de.ivu.mb.ChangeSet`)
                # see https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/hudson/scm/EditType.java
                if paths_key == "paths" and "editType" in path:
                    if path["editType"] == "delete":
                        action = ChangelistItemAction.DELETED
                    elif path["editType"] == "add":
                        action = ChangelistItemAction.ADDED
                # if only strings of `affectedPaths`, this should be the string path
                item_path = (
                    path["file"] if (paths_key == "paths" and "file" in path) else path
                )
                changelist_items.append(
                    ChangelistItem(
                        filepath=item_path,
                        action=action,
                        kind=ChangelistItemKind.FILE
                        if "." in item_path.split("/")[-1]
                        else ChangelistItemKind.DIR,
                    )
                )

            # commit id dependent on _class
            commit_str = None
            if "revision" in log_entry:
                commit_str = str(
                    log_entry["revision"]
                )  # hudson.scm.SubversionChangeLogSet
            elif "commitId" in log_entry:
                commit_str = str(
                    log_entry["commitId"]
                )  # de.ivu.mb.NativeSVNChangeLogSet or hudson.plugins.git.GitChangeSet

            commits.append(
                Commit(
                    commit_str=commit_str,
                    author=log_entry["author"]["fullName"],
                    timestamp=datetime.fromtimestamp(log_entry["timestamp"] / 1000)
                    if "timestamp" in log_entry
                    else datetime.strptime(log_entry["date"], "%Y-%m-%dT%H:%M:%S.%fZ"),
                    changelist=Changelist(items=changelist_items),
                    message=log_entry["msg"],
                )
            )
        return commits
