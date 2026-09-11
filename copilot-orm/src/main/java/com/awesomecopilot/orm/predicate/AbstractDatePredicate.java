package com.awesomecopilot.orm.predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * 日期类条件的公共基类: 维护"当前模式(matchMode)"与"本实例允许的模式集合(候选)"。
 * <p>
 * 字段遮蔽修复说明: 此前 DatePredicate/LocalDatePredicate/LocalDateTimePredicate/
 * LocalTimePredicate 各自又声明了 private matchMode 遮蔽这里的同名字段,
 * 导致子类的 toPredicate() 读子类字段、外部调 setMatchMode()/getMatchMode() 操作的
 * 却是父类字段——模式切换静默失效。子类现在一律通过本类的
 * {@link #setMatchMode}/{@link #getMatchMode} 读写, 全链只有这一份状态。
 */
public abstract class AbstractDatePredicate extends AbstractPredicate{
	
	/**
	 * 当前比较模式。默认值保持历史行为 EARLIER_THAN 不变——SingleDatePredicate 的
	 * 两参构造不显式 set 模式, 依赖该默认; 其余子类构造器均已显式指定, 不受默认值影响
	 */
	private DateMatchMode matchMode = DateMatchMode.EARLIER_THAN;
	
	private List<DateMatchMode> candidateMatchModes = new ArrayList<>();

	public AbstractDatePredicate() {
	}
	
	public AbstractDatePredicate(DateMatchMode matchMode) {
		this.setMatchMode(matchMode);
	}

	protected void addCandidateMatchMode(DateMatchMode... matchModes) {
		for (DateMatchMode matchMode : matchModes) {
			candidateMatchModes.add(matchMode);
		}
	}

	/**
	 * 检查matchMode是否合法(必须在候选集合内), 不合法抛 IllegalArgumentException
	 * 
	 * @return
	 */
	protected void checkDateMatchMode(DateMatchMode matchMode) {
		boolean isLegal = false;
		for (DateMatchMode candidateMatchMode : candidateMatchModes) {
			if (candidateMatchMode.equals(matchMode)) {
				isLegal = true;
				break;
			}
		}

		if(! isLegal) {
			throw new IllegalArgumentException("Can only accept DateMatchMode: " + toCandidateString());
		}
	}

	private String toCandidateString() {
		if (candidateMatchModes.isEmpty()) {
			// 旧实现此处对空 StringBuilder 调 deleteCharAt(-1) 抛 IndexOutOfBounds,
			// 把真正的"模式非法"异常掩盖成莫名其妙的越界
			return "(无候选模式, 该Predicate未正确登记候选)";
		}
		StringBuilder sBuilder = new StringBuilder();
		for (DateMatchMode dateMatchMode : candidateMatchModes) {
			sBuilder.append(dateMatchMode.toString()).append(",");
		}
		sBuilder.deleteCharAt(sBuilder.length() - 1);

		return sBuilder.toString();
	}

	public DateMatchMode getMatchMode() {
		return matchMode;
	}

	public void setMatchMode(DateMatchMode matchMode) {
		this.matchMode = matchMode;
	}

}
