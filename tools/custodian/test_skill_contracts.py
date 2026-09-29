"""Deterministic repository contracts for custodian review skills and fixtures."""
import json
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
SKILLS = ROOT / ".agents" / "skills"
EVALUATIONS = ROOT / "tools" / "custodian" / "skill-evaluations"


class CustodianSkillContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads((EVALUATIONS / "skill-evaluation-manifest.json").read_text())

    def test_manifest_covers_every_custodian_skill_once(self):
        expected = {path.parent.name for path in SKILLS.glob("loandesk-custodian-*/SKILL.md")}
        actual = [entry["name"] for entry in self.manifest["skills"]]
        self.assertEqual(expected, set(actual))
        self.assertEqual(len(actual), len(set(actual)))

    def test_each_skill_has_trigger_and_report_contract(self):
        for entry in self.manifest["skills"]:
            with self.subTest(skill=entry["name"]):
                text = (SKILLS / entry["name"] / "SKILL.md").read_text().lower()
                self.assertRegex(text, rf"(?m)^name:\s*{re.escape(entry['name'])}\s*$")
                self.assertIn(entry["trigger"].lower(), text)
                self.assertRegex(text, r"\b(report|output|produce)\b")

    def test_skills_reference_approved_contracts_not_a_single_plan_file(self):
        for entry in self.manifest["skills"]:
            with self.subTest(skill=entry["name"]):
                text = (SKILLS / entry["name"] / "SKILL.md").read_text().lower()
                self.assertIn("applicable approved custodian requirements", text)
                self.assertNotIn("custodianimplementationplan.md", text)

    def test_each_skill_has_a_blinded_controlled_fixture(self):
        for entry in self.manifest["skills"]:
            with self.subTest(skill=entry["name"]):
                fixture = EVALUATIONS / entry["case"].split("/")[0]
                self.assertTrue((fixture / "case-a").is_dir())
                self.assertTrue((fixture / "case-b").is_dir())
                prompt = (fixture / "review-prompt.txt").read_text().lower()
                self.assertIn(entry["name"], prompt)
                self.assertIn("do not read", prompt)
                self.assertNotIn("expected-results", prompt.replace("expected-results.md", ""))
                self.assertIn("expected", (fixture / "expected-results.md").read_text().lower())

    def test_defective_and_correct_fulfilment_cases_differ_on_both_guards(self):
        defective = (EVALUATIONS / "fulfilment-workflow/case-a/CheckoutService.java").read_text()
        correct = (EVALUATIONS / "fulfilment-workflow/case-b/CheckoutService.java").read_text()
        self.assertNotIn("inWindow &&", defective)
        self.assertNotIn("existingLoanId == null", defective)
        self.assertIn("inWindow &&", correct)
        self.assertIn("existingLoanId == null", correct)

    def test_defective_and_correct_persistence_cases_differ_on_atomicity_and_condition(self):
        defective = (EVALUATIONS / "persistence-condition/case-a/ReturnService.java").read_text()
        correct = (EVALUATIONS / "persistence-condition/case-b/ReturnService.java").read_text()
        self.assertIn("saveLoan", defective)
        self.assertIn('!"LOST"', defective)
        self.assertIn("saveSnapshot", correct)
        self.assertIn('"GOOD".equals(condition)', correct)


if __name__ == "__main__":
    unittest.main(verbosity=2)
