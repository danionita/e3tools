package design.info;

import java.util.Arrays;
import java.util.List;

public class ValueExchange extends Base {
	private static final long serialVersionUID = -7607653966138790703L;
	public String valueObject = null;
	public boolean valueObjectHidden = false;
	public boolean labelHidden = true;
	
	public ValueExchange() {
		formulas.put("CARDINALITY", "1");
		formulas.put("VALUATION", "0");
	}

	@Override
	public Base getCopy() {
		ValueExchange copy = new ValueExchange();
		Base.setCommons(this, copy);
		
		copy.valueObject = valueObject;
		copy.labelHidden = labelHidden;
		copy.valueObjectHidden = valueObjectHidden;

		return copy;
	}
	
	/**
	 * Labels are managed manually by opening/closing E3Properties etc.
	 */
	@Override
	public String toString() {
		String result = "";
		if (!labelHidden && name != null) result += name;
		result += " ";
		if (!valueObjectHidden && valueObject != null) result += " [" + valueObject + "]";
		return result.trim();
	}
	
	@Override
	public List<String> getImmutableProperties() {
		return Arrays.asList(
				"DOTTED",
				"DASHED"
		);
	}

	public boolean isNonOccurring() {
		return formulas.getOrDefault("DASHED", "0").equals("1");
	}
	
	public boolean isHidden() {
		return formulas.getOrDefault("DOTTED", "0").equals("1");
	}
	
	public void setNonOccurring(boolean on) {
		if (on) {
			formulas.put("DASHED", "1");
			formulas.remove("DOTTED");
		} else {
			formulas.remove("DASHED");
		}
	}
	
	public void setHidden(boolean on) {
		if (on) {
			formulas.put("DOTTED", "1");
			formulas.remove("DASHED");
		} else {
			formulas.remove("DOTTED");
		}
	}
}