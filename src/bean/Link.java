package bean;

public class Link {
	private String id;
	private String source;
	private String target;
	private String name;

	public Link id(String id) {
		this.id = id;
		return this;
	}

	public Link name(String id) {
		this.name = id;
		return this;
	}

	public Link source(String id) {
		this.source = id;
		return this;
	}

	public Link target(String id) {
		this.target = id;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
