package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/** 四职业从新角色到学院毕业与独立调查，所有前置均通过正式任务和真实战斗产生。 */
@Import(FormalChapterJourney.class)
class BrightAcademyQuestRuntimeTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	FormalChapterJourney chapter;

	@ParameterizedTest
	@ValueSource(strings = { "WARRIOR", "MAGE", "PRIEST", "RANGER" })
	void academyCoursesProduceFactsThatCompleteBrightQuests(String profession) {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String username = "bq_academy_" + suffix;
		registrations.register(username, "Bright-academy-123", "学院课程回归");
		long owner = accounts.accountId(username);
		characters.create(owner, "学院" + suffix, profession);
		chapter.complete(owner);
	}

}
