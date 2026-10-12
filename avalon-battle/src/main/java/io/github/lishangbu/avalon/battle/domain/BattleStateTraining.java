package io.github.lishangbu.avalon.battle.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** 隔离训练的可变战局存档。记录课程、临时捕捉补给、选定伙伴与动作验收状态；随战局持久化，不修改真实库存。 */
@Getter
@Setter
public class BattleStateTraining {

	/** 学院课程标识和入场选择用于精确重放；空值表示原职业练习。 */
	private String academyCourse;

	/** 捕捉练习的唯一临时库存与公开资料确认；随战局保存，绝不落真实资产表。 */
	private Map<String, Integer> supplies = new HashMap<>();

	private boolean captureInfoViewed;

	private String selectedBall;

	private boolean capturedAfterDamage;

	public boolean capturePractice() {
		return "BQ020".equals(academyCourse);
	}

	private List<String> selectedPartners = new ArrayList<>();

	private List<String> actedUnits = new ArrayList<>();

	private boolean defendedHit;

	private String announcedTarget;

	/** 换怪练习复用隔离存档，但不使用职业试题和首通奖励。 */
	private boolean swapPractice;

	private boolean swapped;

	private String profession;

	private List<Long> rewardPets = new ArrayList<>();

	private int wallHits;

	private boolean areaCompleted;

	private Map<String, Integer> healTicks = new HashMap<>();

	private List<String> markedAttackers = new ArrayList<>();

}
