"""Validate repository contracts for all borrower review skills.

These tests verify skill files and controlled evaluation fixtures. They do not
test Codex's external automatic-selection model; that requires a separate
fresh-agent evaluation and recorded response evidence.
"""
import json
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
SKILLS = ROOT / '.agents' / 'skills'
MANIFEST = ROOT / 'tools' / 'borrower' / 'skill-evaluations' / 'skill-evaluation-manifest.json'


class SkillContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads(MANIFEST.read_text(encoding='utf-8'))

    def test_manifest_covers_every_borrower_skill_once(self):
        expected = {
            path.parent.name
            for path in SKILLS.glob('loandesk-borrower-*/SKILL.md')
        }
        actual = [entry['name'] for entry in self.manifest['skills']]
        self.assertEqual(expected, set(actual))
        self.assertEqual(len(actual), len(set(actual)))

    def test_each_skill_has_frontmatter_trigger_and_output_contract(self):
        for entry in self.manifest['skills']:
            with self.subTest(skill=entry['name']):
                text = (SKILLS / entry['name'] / 'SKILL.md').read_text(encoding='utf-8')
                self.assertRegex(text, r'(?m)^---\s*$')
                self.assertRegex(text, rf'(?m)^name:\s*{re.escape(entry["name"])}\s*$')
                self.assertRegex(text, r'(?m)^description:\s*.+$')
                self.assertIn(entry['trigger'].lower(), text.lower())
                self.assertRegex(text.lower(), r'\b(report|output|produce)\b')

    def test_each_skill_has_a_controlled_evaluation_case(self):
        for entry in self.manifest['skills']:
            with self.subTest(skill=entry['name']):
                case = ROOT / 'tools' / 'borrower' / 'skill-evaluations' / entry['case']
                self.assertTrue(case.is_file(), f'missing evaluation case: {case}')
                text = case.read_text(encoding='utf-8').lower()
                self.assertIn(entry['name'], text)
                self.assertIn('expected', text)

    def test_evaluation_readme_states_automatic_selection_limit(self):
        readme = (ROOT / 'tools' / 'borrower' / 'skill-evaluations' / 'README.md').read_text(
            encoding='utf-8'
        ).lower()
        self.assertIn('automatic skill selection', readme)
        self.assertIn('separate', readme)
        self.assertIn('agent', readme)


if __name__ == '__main__':
    unittest.main(verbosity=2)
