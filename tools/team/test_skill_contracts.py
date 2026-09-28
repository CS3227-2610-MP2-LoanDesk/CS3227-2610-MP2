"""Validate repository contracts for the team-level review skills.

These tests verify skill files and controlled evaluation fixtures. They do not
test an agent's external automatic-selection model; that requires a separate
fresh-agent evaluation and recorded response evidence.
"""
import json
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
SKILLS = ROOT / '.agents' / 'skills'
EVALUATIONS = ROOT / 'tools' / 'team' / 'skill-evaluations'
MANIFEST = EVALUATIONS / 'skill-evaluation-manifest.json'

TEAM_SKILLS = {
    'loandesk-cross-role-scenario-review',
    'loandesk-persistence-failure-test-review',
    'loandesk-release-readiness-review',
    'loandesk-documentation-consistency-review',
}

# Seeded defect and the one shared test that must detect it.
EXECUTABLE_FIXTURES = {
    'cross-role': ('JourneyService.java', 'checkoutLeavesTheRequestAndItsLoanAgreeing', 8),
    'persistence-failure': ('ReturnStore.java', 'aFailedSaveLeavesTheInMemoryStateUnchanged', 6),
}


class SkillContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads(MANIFEST.read_text(encoding='utf-8'))

    def test_manifest_covers_every_team_skill_once(self):
        present = {
            path.parent.name
            for path in SKILLS.glob('loandesk-*/SKILL.md')
            if path.parent.name in TEAM_SKILLS
        }
        actual = [entry['name'] for entry in self.manifest['skills']]
        self.assertEqual(TEAM_SKILLS, present, 'a team skill file is missing')
        self.assertEqual(TEAM_SKILLS, set(actual))
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

    def test_each_skill_has_a_controlled_case_with_a_prompt_and_answer_sheet(self):
        for entry in self.manifest['skills']:
            with self.subTest(skill=entry['name']):
                fixture = (EVALUATIONS / entry['case']).parent
                readme = EVALUATIONS / entry['case']
                self.assertTrue(readme.is_file(), f'missing evaluation case: {readme}')
                self.assertIn(entry['name'], readme.read_text(encoding='utf-8').lower())
                for required in ('requirements.md', 'review-prompt.txt', 'expected-results.md'):
                    self.assertTrue((fixture / required).is_file(),
                                    f'{entry["name"]} is missing {required}')

    def test_every_answer_sheet_is_withheld_from_its_reviewer(self):
        for entry in self.manifest['skills']:
            with self.subTest(skill=entry['name']):
                fixture = (EVALUATIONS / entry['case']).parent
                sheet = (fixture / 'expected-results.md').read_text(encoding='utf-8').lower()
                prompt = (fixture / 'review-prompt.txt').read_text(encoding='utf-8').lower()
                self.assertIn('withhold from reviewer', sheet)
                self.assertIn('requirements.md', prompt)
                self.assertIn('answer sheet', prompt)
                self.assertIn('without editing', prompt)

    def test_every_answer_sheet_names_a_false_positive_trap(self):
        for entry in self.manifest['skills']:
            with self.subTest(skill=entry['name']):
                fixture = (EVALUATIONS / entry['case']).parent
                sheet = (fixture / 'expected-results.md').read_text(encoding='utf-8').lower()
                self.assertIn('false positive', sheet)
                self.assertIn('correct negative', sheet)

    def test_executable_fixtures_keep_exactly_one_seeded_defect(self):
        for folder, (source, failing, count) in EXECUTABLE_FIXTURES.items():
            with self.subTest(fixture=folder):
                fixture = EVALUATIONS / folder
                build = (fixture / 'build.gradle').read_text(encoding='utf-8')
                self.assertIn(f"!= {count}", build,
                              'build must assert the expected test count')
                self.assertIn(f"'{failing}()'", build,
                              'build must assert which test case-a fails')

                defective = next(
                    (fixture / 'case-a').rglob(source)).read_text(encoding='utf-8')
                correct = next(
                    (fixture / 'case-b').rglob(source)).read_text(encoding='utf-8')
                self.assertNotEqual(defective, correct,
                                    'case-a and case-b must differ')
                self.assertLess(len(defective), len(correct),
                                'the seeded defect is an omission, so case-a is shorter')

                tests = (fixture / 'tests').rglob('*ContractTest.java')
                test_text = next(tests).read_text(encoding='utf-8')
                self.assertIn(failing, test_text,
                              'the detecting test must exist in the shared suite')
                self.assertEqual(count, test_text.count('@Test'),
                                 'the shared suite must hold the asserted test count')

    def test_descriptive_fixtures_declare_they_have_no_executable_ground_truth(self):
        for entry in self.manifest['skills']:
            if entry['kind'] != 'descriptive':
                continue
            with self.subTest(skill=entry['name']):
                fixture = (EVALUATIONS / entry['case']).parent
                readme = (fixture / 'README.md').read_text(encoding='utf-8').lower()
                sheet = (fixture / 'expected-results.md').read_text(encoding='utf-8').lower()
                self.assertIn('no executable ground truth', readme)
                self.assertIn('no executable ground truth', sheet)
                for case in ('case-a.md', 'case-b.md'):
                    self.assertTrue((fixture / case).is_file(),
                                    f'{entry["name"]} is missing {case}')

    def test_evaluation_readme_states_automatic_selection_limit(self):
        readme = (EVALUATIONS / 'README.md').read_text(encoding='utf-8').lower()
        self.assertIn('automatic skill selection', readme)
        self.assertIn('separate', readme)
        self.assertIn('agent', readme)


if __name__ == '__main__':
    unittest.main(verbosity=2)
