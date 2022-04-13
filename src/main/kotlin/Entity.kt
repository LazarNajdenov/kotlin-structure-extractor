data class Entity(
    var entityName: String? = null,
    var fullyQualifiedName: String? = null,
    var container: String? = null,
    var type: String? = null,
    var extends: String? = null,
    var implements: MutableList<String>? = null,
    var numberOfMethods: Int? = null,
    var numberOfAttributes: Int? = null,
    var numberOfParameters: Int? = null) {

    fun entityName(entityName: String?) = apply { this.entityName = entityName }
    fun fullyQualifiedName(fullyQualifiedName: String?) = apply { this.fullyQualifiedName = fullyQualifiedName }
    fun container(container: String?) = apply { this.container = container }
    fun type(type: String?) = apply { this.type = type }
    fun extends(extends: String?) = apply { this.extends = extends }
    fun implements(implements: MutableList<String>?) = apply { this.implements = implements }
    fun numberOfMethods(numberOfMethods: Int?) = apply { this.numberOfMethods = numberOfMethods }
    fun numberOfAttributes(numberOfAttributes: Int?) = apply { this.numberOfAttributes = numberOfAttributes }
    fun numberOfParameters(numberOfParameters: Int?) = apply { this.numberOfParameters = numberOfParameters }

}
