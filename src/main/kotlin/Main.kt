import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

private val project by lazy {
    KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.NATIVE_CONFIG_FILES
    ).project
}

fun main(args: Array<String>) {
    val pathToProject: String = System.getProperty("user.dir") + "/projectToAnalyze/"
    val encoding: Charset = Charset.defaultCharset()
    val entities: MutableList<Entity> = mutableListOf()

    File(pathToProject).walk().forEach { file ->
        if (file.name.endsWith(".kt")) {
            val encoded: ByteArray = Files.readAllBytes(Paths.get(file.path))
            val codeString = String(encoded, encoding)
            val ktFile: KtFile = PsiFileFactory
                .getInstance(project)
                .createFileFromText(file.path, KotlinFileType.INSTANCE, codeString) as KtFile
            val packageName: String = ktFile.packageFqName.asString()
            ktFile.children.forEach { psiElement ->
                when (psiElement) {
                    is KtClass -> {
                        addClassEntity(psiElement, entities, packageName)
                    }
                    is KtNamedFunction -> {
                        addMethodEntity(psiElement, entities, packageName)
                    }
                    is KtPackageDirective -> {
                        addPackageEntity(psiElement, entities, packageName)
                    }
                }
            }
        }
    }
    val mapper = jacksonObjectMapper()
    val jsonArray = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entities)
    println(jsonArray)
}

fun addClassEntity( psiElement: KtClass, entities: MutableList<Entity>, packageName: String) {
    val entityName: String? = psiElement.name
    val fullyQualifiedName: String? = psiElement.fqName?.asString()
    val extendedImplementedClasses: List<KtSuperTypeListEntry> = psiElement.superTypeListEntries
    val primaryAttributes: List<KtParameter> = psiElement.primaryConstructorParameters
    val attributes: List<KtProperty> = psiElement.getProperties()
    val totalNumberOfAttributes: Int = primaryAttributes.size + attributes.size
    val methods: List<KtNamedFunction> = psiElement.declarations.filterIsInstance<KtNamedFunction>()
    val numberOfMethods: Int = methods.size
    val (extendedClass, implementedClasses) = retrieveExtendedAndImplementedClasses(extendedImplementedClasses)
    entities.add(
        Entity()
            .entityName(entityName)
            .fullyQualifiedName(fullyQualifiedName)
            .container(packageName)
            .type("CLASS")
            .extends(extendedClass)
            .implements(implementedClasses)
            .numberOfMethods(numberOfMethods)
            .numberOfAttributes(totalNumberOfAttributes)
    )
    addAttributesEntities(primaryAttributes, attributes, entities, fullyQualifiedName)
    addMethodsEntities(methods, entities, fullyQualifiedName)

}

fun addAttributesEntities(primaryAttributes: List<KtParameter>,
                          attributes: List<KtProperty>,
                          entities: MutableList<Entity>,
                          fullyQualifiedName: String?) {
    primaryAttributes.forEach { attribute ->  addAttributeEntity(attribute.name, entities, fullyQualifiedName) }
    attributes.forEach { attribute ->  addAttributeEntity(attribute.name, entities, fullyQualifiedName)}
}

fun addAttributeEntity(attributeName: String?, entities: MutableList<Entity>, fullyQualifiedName: String?) {
    val attributeFullyQualifiedName = "$fullyQualifiedName.$attributeName"
    entities.add(
        Entity()
            .entityName(attributeName)
            .fullyQualifiedName(attributeFullyQualifiedName)
            .container(fullyQualifiedName)
            .type("ATTRIBUTE")
    )
}

fun addMethodsEntities(methods: List<KtNamedFunction>,
                       entities: MutableList<Entity>,
                       fullyQualifiedName: String?) {
    methods.forEach { method -> addMethodEntity(method, entities, fullyQualifiedName) }
}

fun addMethodEntity(
    method: KtNamedFunction,
    entities: MutableList<Entity>,
    fullyQualifiedName: String?
) {
    val methodName: String? = method.name
    val methodFullyQualifiedName: String? = method.fqName?.asString()
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


fun retrieveExtendedAndImplementedClasses(
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
    entities: MutableList<Entity>,
    packageName: String
) {
    val entityName: String = psiElement.name
    if (entityName.isNotEmpty()) {
        entities.add(
            Entity()
                .entityName(entityName)
                .fullyQualifiedName(packageName)
                .container(packageName)
                .type("PACKAGE")
        )
    }
}