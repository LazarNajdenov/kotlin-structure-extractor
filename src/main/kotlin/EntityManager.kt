import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtSuperTypeEntry
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry

class EntityManager(private val entities: MutableList<Entity>) {

    fun removeDuplicatesByFullyQualifiedNameAndType(): List<Entity> {
       return entities.distinctBy { entity -> Pair(entity.fullyQualifiedName, entity.type) }
    }

    fun sortEntitiesByFullyQualifiedName(entities: List<Entity>): List<Entity> {
        return entities.sortedBy { entity -> entity.fullyQualifiedName }
    }

    fun addClassOrObjectEntity(
        psiElement: KtClassOrObject,
        packageName: String,
        isNested: Boolean = false
    ) {
        val entityName: String? = psiElement.name
        val fullyQualifiedName: String = "root." + psiElement.fqName?.asString()
        val extendedImplementedClasses: List<KtSuperTypeListEntry> = psiElement.superTypeListEntries
        val primaryAttributes: List<KtParameter> = psiElement.primaryConstructorParameters
        val attributes: List<KtProperty> = psiElement.declarations.filterIsInstance<KtProperty>()
        val totalNumberOfAttributes: Int = primaryAttributes.size + attributes.size
        val methods: List<KtNamedFunction> = psiElement.declarations.filterIsInstance<KtNamedFunction>()
        val numberOfMethods: Int = methods.size
        val (extendedClass, implementedClasses) = retrieveExtendedAndImplementedClasses(extendedImplementedClasses)
        entities.add(
            Entity()
                .entityName(entityName)
                .fullyQualifiedName(fullyQualifiedName)
                .container(packageName)
                .type(if (psiElement is KtClass) {
                    if (psiElement.isInterface()) {
                        "INTERFACE"
                    } else {
                        "CLASS"
                    }
                } else "OBJECT")
                .isNested(isNested)
                .extends(extendedClass)
                .implements(implementedClasses)
                .numberOfMethods(numberOfMethods)
                .numberOfAttributes(totalNumberOfAttributes)

        )
        val companionObject: List<KtObjectDeclaration> = psiElement.companionObjects
        companionObject.forEach { addClassOrObjectEntity(it, fullyQualifiedName.toString(), true) }

        addAttributesEntities(primaryAttributes, attributes, fullyQualifiedName)
        addMethodsEntities(methods, fullyQualifiedName)

        val innerClasses: List<KtClass> = psiElement.declarations.filterIsInstance<KtClass>()
        innerClasses.forEach { addClassOrObjectEntity(it, fullyQualifiedName.toString(), true) }

    }

    private fun addAttributesEntities(
        primaryAttributes: List<KtParameter>,
        attributes: List<KtProperty>,
        fullyQualifiedName: String?
    ) {
        primaryAttributes.forEach { attribute ->  addAttributeEntity(attribute.name, fullyQualifiedName) }
        attributes.forEach { attribute ->  addAttributeEntity(attribute.name, fullyQualifiedName)}
    }

    private fun addAttributeEntity(
        attributeName: String?,
        fullyQualifiedName: String?
    ) {
        val attributeFullyQualifiedName = "$fullyQualifiedName.$attributeName"
        entities.add(
            Entity()
                .entityName(attributeName)
                .fullyQualifiedName(attributeFullyQualifiedName)
                .container(fullyQualifiedName)
                .type("ATTRIBUTE")
        )
    }

    private fun addMethodsEntities(
        methods: List<KtNamedFunction>,
        fullyQualifiedName: String?,
        isTopLevel: Boolean = false
    ) {
        methods.forEach { method -> addMethodEntity(method, fullyQualifiedName, isTopLevel) }
    }

    private fun addMethodEntity(
        method: KtNamedFunction,
        fullyQualifiedName: String?,
        isTopLevel: Boolean = false
    ) {
        val methodName: String? = method.name
        val methodFullyQualifiedName: String? = if (isTopLevel) "$fullyQualifiedName.$methodName" else "root." + method.fqName?.asString()
        val numberOfParameters: Int = method.valueParameters.size
        entities.add(
            Entity()
                .entityName(methodName)
                .fullyQualifiedName(methodFullyQualifiedName)
                .container(fullyQualifiedName)
                .type("METHOD")
                .numberOfParameters(numberOfParameters)
        )
    }


    private fun retrieveExtendedAndImplementedClasses(
        extendedImplementedClasses: List<KtSuperTypeListEntry>,
    ): Pair<String?, MutableList<String>> {
        var extendedClass: String? = null
        val implementedClasses: MutableList<String> = mutableListOf()
        extendedImplementedClasses.forEach { parent ->
            when (parent) {
                is KtSuperTypeCallEntry -> {
                    extendedClass = parent.typeReference?.text.toString()
                }
                is KtSuperTypeEntry -> {
                    implementedClasses.add(parent.typeReference?.text.toString())
                }
            }
        }
        return Pair(extendedClass, implementedClasses)
    }

    fun addPackageEntity(
        psiElement: KtPackageDirective,
        packageName: String
    ) {
        val entityName: String = psiElement.name
        if (entityName.isNotEmpty()) {
            val containerFullyQualified: String = packageName.dropLast(entityName.length + 1)
            entities.add(
                Entity()
                    .entityName(entityName)
                    .fullyQualifiedName(packageName)
                    .container(containerFullyQualified)
                    .type("PACKAGE")
            )
            if (containerFullyQualified.isNotEmpty()) addRecursivePackageEntity(containerFullyQualified)
        }
    }
    private fun addRecursivePackageEntity(container: String) {
        var packageName: String = container
        val packages: List<String> = packageName
            .split(".")
            .asReversed()
        if (packages.isNotEmpty()) {
            for ((index, entityName) in packages.withIndex()) {
                val containerFullyQualified: String = packages
                    .subList(index + 1, packages.size)
                    .asReversed()
                    .joinToString(separator = ".")
                entities.add(
                    Entity()
                        .entityName(entityName)
                        .fullyQualifiedName(packageName)
                        .container(containerFullyQualified)
                        .type("PACKAGE")
                )
                packageName = containerFullyQualified
            }
        }
    }

    fun addPackageObjectEntity(packageName: String, ktFile: KtFile) {
        val entityName = "PackageObject"
        val packageObjectFullyQualifiedName = "$packageName.$entityName"
        val topLevelMethods: List<KtNamedFunction> = ktFile.declarations.filterIsInstance<KtNamedFunction>()
        entities.add(
            Entity()
                .entityName(entityName)
                .fullyQualifiedName(packageObjectFullyQualifiedName)
                .container(packageName)
                .type("OBJECT")
                .numberOfMethods(topLevelMethods.size)
        )
        addMethodsEntities(topLevelMethods, packageObjectFullyQualifiedName, true)
    }
}