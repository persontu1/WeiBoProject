package bean;

public class Node {
	private String category;
	private boolean draggable = true;
	private String id;
	private String name;
	private String symbolSize = "10";

	public Node category(String c) {
		this.category = c;
		return this;
	}

	public Node draggable(boolean d) {
		this.draggable = d;
		return this;
	}

	public Node id(String id) {
		this.id = id;
		return this;
	}

	public Node name(String id) {
		this.name = id;
		return this;
	}

	public Node symbolSize(String id) {
		this.symbolSize = id;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public boolean isDraggable() {
		return draggable;
	}

	public void setDraggable(boolean draggable) {
		this.draggable = draggable;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSymbolSize() {
		return symbolSize;
	}

	public void setSymbolSize(String symbolSize) {
		this.symbolSize = symbolSize;
	}
}
