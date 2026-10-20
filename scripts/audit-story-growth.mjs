
// Read production content without regenerating or mutating it. These are quest-only
// budgets, not proof of natural play: combat, optional quests and dungeon XP differ.
import { readContent, storyContent } from './content-files.mjs';
const { thresholds } = readContent('rules/experience.json');
const { steps } = storyContent();
const { profiles } = readContent('battle/wilds.json');
const xp = (prefix, first, last) => steps
  .filter((step) => step.quest?.match(new RegExp(`^${prefix}\\d{3}$`)))
  .filter((step) => Number(step.quest.slice(1)) >= first && Number(step.quest.slice(1)) <= last)
  .flatMap((step) => step.effects)
  .filter((effect) => effect.kind === 'EXPERIENCE')
  .reduce((sum, effect) => sum + effect.amount, 0);
const level = (earned) => thresholds.filter((threshold) => earned >= threshold).length;
let earned = xp('O', 1, 15) + xp('N', 1, 25);
console.log('Quest-only XP budget (excludes all combat and optional quests)');
for (const prefix of ['N', 'U', 'S', 'F']) {
  console.log(JSON.stringify({ region: prefix, mainQuestExperience: xp(prefix, 1, 25),
    optionalQuestExperience: xp(prefix, 26, 60) }));
}
console.log(JSON.stringify({ through: 'O015 + N025', experience: earned, level: level(earned) }));
for (const prefix of ['U', 'S']) {
  let independent = earned;
  for (let last = 5; last <= 25; last += 5) {
    independent += xp(prefix, last - 4, last);
    console.log(JSON.stringify({ independentRoute: prefix, through: `${prefix}${String(last).padStart(3, '0')}`,
      experience: independent, level: level(independent) }));
  }
}
earned += xp('U', 1, 25) + xp('S', 1, 25);
for (const [prefix, last] of [['F', 25], ['J', 30], ['E', 15]]) {
  earned += xp(prefix, 1, last);
  console.log(JSON.stringify({ through: `${prefix}${String(last).padStart(3, '0')}`, experience: earned, level: level(earned) }));
}

// A reward-only estimate, not a claim that the party can win these battles.
// Use the production level correction and bracket enemy levels in each profile.
function fightsToLevel(start, target, profile, count, upperEnemyLevel) {
  const enemies = Object.values(profile.species);
  const enemyLevel = upperEnemyLevel ? Math.max(...enemies.map((entry) => entry.maxLevel))
    : Math.min(...enemies.map((entry) => entry.minLevel));
  let experience = start;
  let fights = 0;
  while (level(experience) < target && fights < 10000) {
    const correction = Math.max(0.5, Math.min(1.5, 1 + 0.02 * (enemyLevel - level(experience))));
    experience += count * Math.floor(enemyLevel * 10 * profile.experienceMultiplier * correction);
    fights++;
  }
  return fights;
}
const opening = xp('O', 1, 15) + xp('N', 1, 25);
for (const prefix of ['U', 'S']) {
  const profile = profiles[prefix + '_APPROACH_PATROL'];
  const start = opening + xp(prefix, 1, 12);
  console.log(JSON.stringify({ estimate: 'independent-city-24', region: prefix, startExperience: start,
    extraFights: [fightsToLevel(start, 24, profile, 2, true), fightsToLevel(start, 24, profile, 2, false)],
    excludes: 'combat already required by quests, optional rewards, losses, supplies and recovery' }));
}
const deep = profiles.U_DEEP_PATROL;
console.log(JSON.stringify({ estimate: 'level-24-to-28',
  extraFights: [fightsToLevel(thresholds[23], 28, deep, 2, true), fightsToLevel(thresholds[23], 28, deep, 2, false)],
  excludes: 'quest and dungeon rewards, losses, supplies and recovery' }));
for (const [from, to, code] of [[28, 31, 'F_ASH_PATROL'], [31, 33, 'F_CORE_PATROL'], [33, 36, 'F_AFFECTED_PACK'], [36, 40, 'J_PERIMETER_PATROL'], [40, 45, 'E_RESIDUAL_PATROL']]) {
  const profile = profiles[code];
  console.log(JSON.stringify({ estimate: `level-${from}-to-${to}`, encounter: code,
    extraFights: [fightsToLevel(thresholds[from - 1], to, profile, 2, true),
      fightsToLevel(thresholds[from - 1], to, profile, 2, false)],
    excludes: 'quest and dungeon rewards, losses, supplies and recovery' }));
}

// Optional profession budget, using LifeSkillRules.threshold/gain (half-points).
// Greedily choose a current-level basic recipe by direct ingredients per point;
// this is a reproducible estimate, not an optimal route or a natural-play result.
const production = readContent('professions/life-production.json');
const professionThreshold = (value) => 256 * (2 ** (value - 1) - 1);
const professionLevel = (halfPoints) => {
  let value = 1;
  while (value < 10 && halfPoints >= professionThreshold(value + 1)) value++;
  return value;
};
const operations = {};
const ingredients = {};
let halfPoints = 0;
while (professionLevel(halfPoints) < 9) {
  const current = professionLevel(halfPoints);
  const cost = (recipe) => Object.values(recipe.ingredients).reduce((sum, count) => sum + count, 0) / recipe.points;
  const recipe = production.recipes
    .filter((entry) => entry.skill === 'LEATHERWORK' && entry.level === current && !entry.blueprint && entry.points > 0)
    .sort((a, b) => cost(a) - cost(b) || a.id.localeCompare(b.id))[0];
  if (!recipe) throw new Error(`No automatically learned leatherwork recipe at level ${current}`);
  halfPoints += recipe.points * 2;
  operations[recipe.id] = (operations[recipe.id] ?? 0) + 1;
  for (const [item, count] of Object.entries(recipe.ingredients))
    ingredients[item] = (ingredients[item] ?? 0) + count;
}
console.log(JSON.stringify({ estimate: 'optional-leatherwork-1-to-9', operations,
  totalCrafts: Object.values(operations).reduce((sum, count) => sum + count, 0), ingredients,
  excludes: 'material production, gathering, trading, travel, tool loss and elapsed time' }));
