# Copyright (c) 2019-present. The coop authors.
import glob
import os
import re
import xml.etree.ElementTree as ET

from abc import ABC, abstractmethod
from typing import List, Optional, Union
from pathlib import Path

from ...models.scm.base import Repository, Commit


class Hook(ABC):
    """
    Hook interface for execution inside walker.
    """

    def __init__(
            self,
            repository: Repository,
            output_path: Optional[str] = None,
            git_client=None,
            java11_special: bool = False
    ) -> None:
        self.repository = repository
        self.output_path = output_path
        self.git_client = git_client
        self.java11_special = java11_special

    @abstractmethod
    def run(self, commit: Commit) -> int:
        pass

    def getExcludesFilePath(self, pom_path):
        pass

    def _update_pom_special(self, plugins_node):
        pass

    # search for child node in element
    def _search_node(self, el: ET.Element, node: str, do_not_insert = False):
        searched_node = None
        for child in el:
            tag = child.tag
            if "{" in tag:
                _, _, tag = tag.rpartition("}")
            if tag == node:
                searched_node = child
                break
        if do_not_insert or searched_node is not None:
            return searched_node
        else:
            searched_node = ET.Element(node)
            el.append(searched_node)
            return searched_node

    def _remove_artifact_group(self, el: ET.Element, tag: str, artifact: str, group: str):
        node = self._search_artifact_group(el, tag, artifact, group)
        el.remove(node)

    # search for node with tag containing artifact and group
    def _search_artifact_group(self, el: ET.Element, tag: str, artifact: str, group: str):
        searched_node = None
        for child in el:
            if child.tag.endswith(tag):
                found_group = True  # apparently it is optional to specify the group
                found_artifact = False
                for inner in child:
                    if inner.tag.endswith("groupId") and not inner.text == group:
                        found_group = False
                    if inner.tag.endswith("artifactId") and inner.text == artifact:
                        found_artifact = True
                if found_group and found_artifact:
                    searched_node = child
                    break
        if searched_node is not None:
            return searched_node
        else:
            searched_node = ET.Element(tag)
            group_node = ET.Element("groupId")
            group_node.text = group
            artifact_node = ET.Element("artifactId")
            artifact_node.text = artifact
            searched_node.append(group_node)
            searched_node.append(artifact_node)
            el.append(searched_node)
            return searched_node

    def _update_pom(self):
        """
        Adjust `pom.xml` to contain compatible version of maven surefire plugin.

        :return:
        """
        filename = "pom.xml"
        filepaths = glob.glob(self.repository.path + "/**/" + filename, recursive=True)

        for filepath in filepaths:
            if os.path.isfile(filepath):

                def _namespace(el: ET.Element):
                    m = re.match(r"\{.*\}", el.tag)
                    return m.group(0)[1:-1] if m else ""

                # get ns from xml
                try:
                    tree = ET.parse(filepath)
                except:
                    return True
                ns = _namespace(tree.getroot())
                # set default namespace
                ET.register_namespace("", ns)
                # re-parse xml
                tree = ET.parse(filepath)
                root = tree.getroot()

                # # Special Case: 'org.sonatype.oss:oss-parent' as parent silently activates various plugins we do not want to execute
                # oss_parent = self._search_artifact_group(root, 'parent', 'oss-parent', 'org.sonatype.oss')
                # root.remove(oss_parent)
                #
                # # Special Case: 'org.jenkins-ci.plugins:plugin' as parent silently activates various plugins we do not want to execute
                # oss_parent = self._search_artifact_group(root, 'parent', 'plugin', 'org.jenkins-ci.plugins')
                # root.remove(oss_parent)
                #
                # # Special Case: packaging 'maven-plugin' calls plugin-plugin:descriptor, which we do not want
                # packaging = self._search_node(root, 'packaging', do_not_insert=True)
                # if packaging is not None and packaging.text == 'maven-plugin':
                #     root.remove(packaging)
                #
                # # Special Case: packaging 'hpi' leads to error
                # packaging = self._search_node(root, 'packaging', do_not_insert=True)
                # if packaging is not None and packaging.text == 'hpi':
                #     root.remove(packaging)

                build_node = self._search_node(root, "build")
                plugins_mgmt_node = self._search_node(build_node, "pluginManagement")
                plugins_node = self._search_node(plugins_mgmt_node, "plugins")

                # add maven-compiler-plugin with source configuration for java 11
                compiler_plugin = self._search_artifact_group(plugins_node, 'plugin', 'maven-compiler-plugin',
                                                              'org.apache.maven.plugins')
                configuration = self._search_node(compiler_plugin, 'configuration')
                source = self._search_node(configuration, 'source')
                source.text = "1.8"
                target = self._search_node(configuration, 'target')
                target.text = "1.8"

                # add maven-surefire plugin
                plugin = self._search_artifact_group(plugins_node, "plugin", "maven-surefire-plugin",
                                                     "org.apache.maven.plugins")
                version = self._search_node(plugin, "version")
                version.text = "3.0.0-M5"  # we need to enforce this version, since excludesFile is not possible in earlier versions

                # remove configurations node, re-added later
                configurations_node = self._search_node(plugin, "configuration")
                plugin.remove(configurations_node)

                # create excludesFile
                excludesFile_path = self.getExcludesFilePath(filepath)
                # create excludesFile
                if excludesFile_path is not None:
                    # set surefire's excludesFile
                    configuration = self._search_node(plugin, "configuration")
                    excludesFile = self._search_node(configuration, "excludesFile")

                    excludesFile_path = excludesFile_path
                    excludesFile.text = str(excludesFile_path)

                    if not os.path.exists(excludesFile_path):
                        os.mknod(excludesFile_path)

                dependencies_node = self._search_node(plugin, 'dependencies')

                # append surefire-logger-api #edgecase
                dependency = self._search_artifact_group(dependencies_node, "dependency", "surefire-logger-api",
                                                         "org.apache.maven.surefire")
                version = self._search_node(dependency, "version")
                version.text = "3.0.0-M5"

                # append surefire-api #edgecase
                dependency = self._search_artifact_group(dependencies_node, "dependency", "surefire-api",
                                                         "org.apache.maven.surefire")
                version = self._search_node(dependency, "version")
                version.text = "3.0.0-M5"

                plugins_node = self._search_node(build_node, "plugins")

                # remove surefire version in potential node in plugins outside pluginManagement
                plugin = self._search_artifact_group(plugins_node, "plugin", "maven-surefire-plugin",
                                                     "org.apache.maven.plugins")
                version = self._search_node(plugin, "version")
                plugin.remove(version)

                # remove configurations node
                configurations_node = self._search_node(plugin, "configuration")
                plugin.remove(configurations_node)

                # remove surefire version in properties if present
                properties_node = self._search_node(root, "properties")
                surefire_version_node = self._search_node(properties_node, "surefire.version")
                properties_node.remove(surefire_version_node)

                dependencies_node = self._search_node(root, "dependencies")

                # set dependency on JUnit 4.12
                junit_dependency = self._search_artifact_group(dependencies_node, 'dependency', 'junit', 'junit')
                version = self._search_node(junit_dependency, 'version')
                version.text = '4.12'

                # add dependency on hibernate #edgecase
                hibernate_dependency = self._search_artifact_group(dependencies_node, 'dependency',
                                                                   'hibernate-validator', 'org.hibernate')
                version = self._search_node(hibernate_dependency, 'version')
                version.text = "6.0.2.Final"

                if self.java11_special:
                    javax_annotation_dependency = self._search_artifact_group(dependencies_node, 'dependency',
                                                                              'javax.annotation-api',
                                                                              'javax.annotation')
                    version = self._search_node(javax_annotation_dependency, 'version')
                    version.text = "1.3.1"

                    javax_xml_bind_dependency = self._search_artifact_group(dependencies_node, 'dependency', 'jaxb-api',
                                                                            'javax.xml.bind')
                    version = self._search_node(javax_xml_bind_dependency, 'version')
                    version.text = "2.3.0"

                    sun_tools = self._search_artifact_group(dependencies_node, 'dependency', 'tools', 'com.sun')
                    dependencies_node.remove(sun_tools)

                self._update_pom_special(plugins_node)

                # write back to file
                tree.write(filepath)
                
        return False

    def cleanExcludesFiles(self):
        filename = "pom.xml"
        filepaths = glob.glob(self.repository.path + "/**/" + filename, recursive=True)
        for filepath in filepaths:
            excludesFile_path = self.getExcludesFilePath(filepath)
            # clean excludesFile
            if excludesFile_path is not None:
                if os.path.exists(excludesFile_path):
                    os.unlink(excludesFile_path)


class Walker(ABC):
    """
    Walker base class to replay repository history.
    """

    def __init__(
            self,
            repository: Repository,
            commit_list: Optional[List[str]],
            include_merge_commits: bool = False,
            branch: Optional[str] = None,
            num_commits: Optional[int] = 10,
            end_commit: Optional[Commit] = None,
            pre_hooks: Optional[List[Hook]] = None,
            hooks: Optional[List[Hook]] = None,
            post_hooks: Optional[List[Hook]] = None,
    ) -> None:
        """
        Constructor for walkers.

        :param repository: A **local** git repository.
        :param start_commit:
        :param include_merge_commits:
        :param branch:
        :param num_commits:
        :param end_commit:
        :param pre_hooks:
        :param hooks:
        :param post_hooks:
        """
        self.repository = repository
        self.commit_list = commit_list
        self.include_merge_commits = include_merge_commits
        self.num_commits = num_commits
        self.end_commit = end_commit
        self.branch = branch
        self.pre_hooks: List[Hook] = pre_hooks if pre_hooks else []
        self.hooks: List[Hook] = hooks if hooks else []
        self.post_hooks: List[Hook] = post_hooks if post_hooks else []

    @abstractmethod
    def walk(self) -> None:
        """
        Step through the repository history and execute hooks before, while and after stepping.
        """
        pass
