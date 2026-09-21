"""Exercise real Git hooks in disposable repositories, without touching user data."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
HOOKS = ROOT / 'tools/borrower/hooks'


class HookTests(unittest.TestCase):
    def setUp(self):
        scratch = ROOT / 'build/borrower-hook-tests'
        scratch.mkdir(parents=True, exist_ok=True)
        self.tmp = tempfile.TemporaryDirectory(dir=scratch)
        self.addCleanup(self.tmp.cleanup)
        self.repo = Path(self.tmp.name)
        self.git('init', '-q')
        self.git('config', 'core.autocrlf', 'false')
        self.git('config', 'core.hooksPath', HOOKS.as_posix())

    def git(self, *args):
        return subprocess.run(['git', *args], cwd=self.repo, check=True,
                              capture_output=True, text=True)

    def stage(self, path, text):
        target = self.repo / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding='utf-8', newline='\n')
        self.git('add', '-f', '--', path)
        return target

    def hook(self, name='pre-commit'):
        before = self.git('diff', '--cached', '--binary').stdout
        result = subprocess.run(['git', 'hook', 'run', name], cwd=self.repo,
                                capture_output=True, text=True)
        self.assertEqual(before, self.git('diff', '--cached', '--binary').stdout)
        return result

    def test_clean_borrower_change(self):
        self.stage('src/main/java/loandesk/features/borrower/Good.java', 'class Good {}\n')
        self.assertEqual(0, self.hook().returncode)

    def test_empty_index(self):
        self.assertEqual(0, self.hook().returncode)

    def test_force_added_data_blocked_without_contents(self):
        self.stage('.gitignore', 'data/\n')
        self.stage('data/loandesk.json', '{"private":"synthetic-secret"}\n')
        result = self.hook()
        self.assertNotEqual(0, result.returncode)
        self.assertIn('local data', result.stderr)
        self.assertNotIn('synthetic-secret', result.stdout + result.stderr)

    def test_whitespace_blocked(self):
        self.stage('notes with spaces.txt', 'bad trailing whitespace  \n')
        self.assertNotEqual(0, self.hook().returncode)

    def test_each_conflict_marker_blocked(self):
        for marker in ('<<<<<<< HEAD', '=======', '>>>>>>> branch', '||||||| base'):
            with self.subTest(marker=marker):
                self.stage('notes.txt', marker + '\n')
                self.assertNotEqual(0, self.hook().returncode)

    def test_shared_and_other_role_only_warn(self):
        for path in ('src/main/java/loandesk/domain/Shared.java',
                     'src/main/java/loandesk/features/supervisor/View.java',
                     'src/test/java/loandesk/features/custodian/Tests.java'):
            self.stage(path, '// example\n')
        result = self.hook()
        self.assertEqual(0, result.returncode)
        self.assertEqual(3, result.stderr.count('WARNING:'))

    def test_unstaged_defect_does_not_block_clean_index(self):
        path = self.stage('notes.txt', 'clean staged text\n')
        path.write_text('<<<<<<< HEAD\n', encoding='utf-8')
        self.assertEqual(0, self.hook().returncode)
        self.assertEqual('<<<<<<< HEAD\n', path.read_text())

    def test_unstaged_fix_does_not_hide_staged_defect(self):
        path = self.stage('notes.txt', '<<<<<<< HEAD\n')
        path.write_text('clean unstaged text\n', encoding='utf-8')
        self.assertNotEqual(0, self.hook().returncode)

    def test_pre_push_propagates_success_and_failure_and_arguments(self):
        for code in (0, 7):
            with self.subTest(code=code):
                if os.name == 'nt':
                    (self.repo / 'gradlew.bat').write_text(
                        '@echo off\n> called.txt echo %*\nexit /b ' + str(code) + '\n')
                else:
                    (self.repo / 'gradlew').write_text(
                        '#!/bin/sh\nprintf "%s\\n" "$*" > called.txt\nexit ' + str(code) + '\n')
                result = self.hook('pre-push')
                self.assertEqual(code, result.returncode, result.stdout + result.stderr)
                self.assertEqual('clean test --no-daemon',
                                 (self.repo / 'called.txt').read_text().strip())


if __name__ == '__main__':
    unittest.main(verbosity=2)
