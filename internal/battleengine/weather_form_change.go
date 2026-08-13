package battleengine

// synchronizeWeatherForms 按当前有效普通天气同步所有上场成员的天气形态。
//
// 规则读取 effectiveWeather，因此天气封锁会让成员回到默认形态，而不是删除环境中的原始天气。该函数必须在
// 天气建立、天气结束、强天气来源变化及带有天气封锁特性的成员换入后调用，确保状态快照始终与有效环境一致。
func synchronizeWeatherForms(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	weather := effectiveWeather(state)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || member.WeatherFormChange == nil {
				continue
			}
			targetCreatureID := member.WeatherFormChange.DefaultCreatureID
			if weather != nil {
				for _, target := range member.WeatherFormChange.Targets {
					if target.Weather == weather.Kind {
						targetCreatureID = target.CreatureID
						break
					}
				}
			}
			if targetCreatureID == member.CreatureID {
				continue
			}
			profile, profileFound := member.formProfile(targetCreatureID)
			if !profileFound {
				continue
			}
			previousCreatureID := member.CreatureID
			member = applyFormProfile(member, profile)
			state.replaceMember(side.Side, member)
			events = append(events, FormChangedEvent{
				Type: EventKindFormChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Member: MemberRef{Side: side.Side, Position: member.Position}, FromCreatureID: previousCreatureID,
				ToCreatureID: member.CreatureID, Reason: FormChangeReasonWeatherAbility,
			})
		}
	}
	return state, events
}
