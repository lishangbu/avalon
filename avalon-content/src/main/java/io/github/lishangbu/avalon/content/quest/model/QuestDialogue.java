package io.github.lishangbu.avalon.content.quest.model;

/** offer为接取前，accepted为接取回执，progress为目标未齐，ready为待交付，revisit为再访，resume为重接。 */
public record QuestDialogue(String offer, String accepted, String progress, String ready, String revisit,
		String resume) {
}
