// Fast seeded route model using production quest, experience and wild-encounter content.
// It estimates successful combat progression; it does not execute combat or model supplies.
import { readContent, storyContent } from "./content-files.mjs";
import { seededRandom } from "./growth-model.mjs";

const args = new Map();
for (let i = 2; i < process.argv.length; i++) {
  if (!process.argv[i].startsWith("--") || !process.argv[i + 1])
    throw new Error(`Expected --name value, got ${process.argv[i] ?? ""}`);
  args.set(process.argv[i++].slice(2), process.argv[i]);
}
for (const key of args.keys()) {
  if (!["trials", "seed"].includes(key))
    throw new Error(`Unknown option --${key}`);
}
const trials = Number(args.get("trials") ?? 1000);
const baseSeed = Number(args.get("seed") ?? 20261025);
if (!Number.isSafeInteger(trials) || trials < 1 || trials > 100_000)
  throw new Error("--trials must be 1..100000");
if (!Number.isSafeInteger(baseSeed) || baseSeed < 0)
  throw new Error("--seed must be a non-negative safe integer");

const { thresholds } = readContent("rules/experience.json");
const { steps } = storyContent();
const { profiles } = readContent("battle/wilds.json");
const levelAt = (experience) =>
  thresholds.filter((threshold) => experience >= threshold).length;
const routeQuestExperience = steps
  .filter((step) =>
    /^O\d{3}$|^N\d{3}$|^[USFJ]\d{3}$|^E00[1-8]$|^E010$/.test(step.quest ?? ""),
  )
  .flatMap((step) => step.effects)
  .filter((effect) => effect.kind === "EXPERIENCE")
  .reduce((sum, effect) => sum + effect.amount, 0);

function encounter(profile, next) {
  const species = Object.values(profile.species);
  const totalWeight = species.reduce((sum, entry) => sum + entry.weight, 0);
  const roll = next() * totalWeight;
  let cumulative = 0;
  const selected =
    species.find((entry) => (cumulative += entry.weight) > roll) ??
    species.at(-1);
  return (
    selected.minLevel +
    Math.floor(next() * (selected.maxLevel - selected.minLevel + 1))
  );
}

function simulate(seed) {
  const next = seededRandom(seed);
  let experience = 0;
  let gold = 0;
  let fights = 0;
  let stageStart = 0;
  const checkpoints = [];
  const awardQuests = (name, patterns) => {
    const award = steps
      .filter((step) =>
        patterns.some((pattern) => pattern.test(step.quest ?? "")),
      )
      .flatMap((step) => step.effects)
      .filter((effect) => effect.kind === "EXPERIENCE")
      .reduce((sum, effect) => sum + effect.amount, 0);
    experience = Math.min(thresholds.at(-1), experience + award);
    checkpoints.push({
      stage: name,
      level: levelAt(experience),
      questXp: award,
      patrolFights: fights - stageStart,
    });
    stageStart = fights;
  };
  const train = (target, profileId) => {
    const profile = profiles[profileId];
    if (!profile) throw new Error(`Missing encounter profile ${profileId}`);
    while (levelAt(experience) < target) {
      let fightXp = 0;
      let fightGold = 0;
      // Patrols in the audited route contain two wild opponents.
      for (let i = 0; i < 2; i++) {
        const enemyLevel = encounter(profile, next);
        const correction = Math.max(
          0.5,
          Math.min(1.5, 1 + 0.02 * (enemyLevel - levelAt(experience))),
        );
        fightXp += Math.floor(
          enemyLevel * 10 * profile.experienceMultiplier * correction,
        );
        fightGold += Math.floor((enemyLevel + 2) / 3) * profile.goldMultiplier;
      }
      experience = Math.min(thresholds.at(-1), experience + fightXp);
      gold += fightGold;
      fights++;
      if (fights > 100_000)
        throw new Error(`Growth route did not converge at ${profileId}`);
    }
    checkpoints.push({
      stage: `${profileId} -> ${target}`,
      level: levelAt(experience),
      questXp: 0,
      patrolFights: fights - stageStart,
    });
    stageStart = fights;
  };

  awardQuests("Opening and Nom", [/^O\d{3}$/, /^N\d{3}$/]);
  awardQuests("Undine and Sylph", [/^U\d{3}$/, /^S\d{3}$/]);
  train(28, "U_DEEP_PATROL");
  awardQuests("Salamander", [/^F\d{3}$/]);
  train(36, "F_AFFECTED_PACK");
  awardQuests("Joint finale", [/^J\d{3}$/]);
  train(40, "J_PERIMETER_PATROL");
  awardQuests("Postgame E001-E008 and E010", [/^E00[1-8]$/, /^E010$/]);
  train(50, "E_RESIDUAL_PATROL");
  return { experience, level: levelAt(experience), gold, fights, checkpoints };
}

const results = Array.from({ length: trials }, (_, index) =>
  simulate(baseSeed + index),
);
const stats = (key) => {
  const sorted = results.map((result) => result[key]).sort((a, b) => a - b);
  return {
    min: sorted[0],
    p10: sorted[Math.floor((sorted.length - 1) * 0.1)],
    median: sorted[Math.floor((sorted.length - 1) * 0.5)],
    p90: sorted[Math.floor((sorted.length - 1) * 0.9)],
    max: sorted.at(-1),
  };
};
console.log(
  JSON.stringify(
    {
      model: "seeded successful patrol simulation",
      content:
        "production quest experience, experience thresholds, weighted encounter species/levels and battle experience/gold formulas",
      assumptions: [
        "all listed regional quests, including optional quests, are completed and their experience is awarded",
        "each patrol win defeats two enemies",
        "no losses, capture costs, supplies, travel, manufacturing or random loot",
        "the model does not prove combat survivability or full route reachability",
      ],
      trials,
      seed: baseSeed,
      routeQuestExperience,
      level50ExperienceThreshold: thresholds.at(-1),
      patrolFights: stats("fights"),
      patrolGold: stats("gold"),
      sample: results[0],
    },
    null,
    2,
  ),
);
