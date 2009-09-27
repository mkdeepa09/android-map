package tac.program.model;

public enum AtlasOutputFormat {

	TaredAtlas("TrekBuddy tared atlas"), // 
	UntaredAtlas("TrekBuddy untared atlas"), //
	AndNav("Android AndNav2 atlas format"), //
	BigPlanet("Android BigPlanet SQLite DB"), //
	OziPng("OziExplorer (PNG & MAP)");

	private final String displayName;

	private AtlasOutputFormat(String displayName) {
		this.displayName = displayName;
	}

	public String toString() {
		return displayName;
	}
}
