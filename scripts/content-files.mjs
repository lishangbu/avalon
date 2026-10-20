import fs from 'node:fs';
import path from 'node:path';
export const contentDirectory = path.resolve(import.meta.dirname, '../avalon-content/src/main/resources/content');
export function readContent(relativePath) {
  return JSON.parse(fs.readFileSync(path.join(contentDirectory, relativePath), 'utf8'));
}
export function contentFiles(directory) {
  return fs.readdirSync(path.join(contentDirectory, directory), { recursive: true })
    .filter(file => file.endsWith('.json')).sort().map(file => path.posix.join(directory, file.replaceAll('\\', '/')));
}
export function storyContent() {
  const quests = contentFiles('quests').filter(file => file !== 'quests/tags.json')
    .flatMap(file => readContent(file).quests);
  return { quests: quests.map(({ steps, ...rule }) => rule),
    steps: quests.flatMap(quest => quest.steps.map(({ conversations, ...step }) => step)),
    dialogues: Object.fromEntries(quests.flatMap(quest => quest.steps.map(step => [step.id, step.conversations]))) };
}
