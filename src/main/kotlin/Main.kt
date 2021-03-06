import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPackageDirective
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
    val start = System.nanoTime()
    val encoding: Charset = Charset.defaultCharset()
    val pathToProject: String = if (args.size == 1) args[0] else System.getProperty("user.dir") + "/projectToAnalyze/"
    val pathToOutput: String = System.getProperty("user.dir") + "/output/entities.json"
    val jsonFile = File(pathToOutput)
    jsonFile.createNewFile()
    val entityManager = EntityManager(mutableListOf())

    File(pathToProject).walk().forEach { file ->
        if (file.isFile && file.name.endsWith(".kt")) {
            val ktFile: KtFile = generateKtFile(file, encoding)
            val ktPackageName: String = ktFile.packageFqName.asString()
            val packageName: String = "root" + if (ktPackageName.isNotEmpty()) {
                ".$ktPackageName"
            } else {
                ""
            }
            if (ktFile.hasTopLevelCallables()) entityManager.addPackageObjectEntity(packageName, ktFile)
            ktFile.children.forEach { psiElement ->
                when (psiElement) {
                    is KtPackageDirective -> {
                        entityManager.addPackageEntity(psiElement, packageName)
                    }
                    is KtClassOrObject -> {
                        entityManager.addClassOrObjectEntity(psiElement, packageName)
                    }
                }
            }
        }
    }
    val uniqueEntities: List<Entity> = entityManager.removeDuplicatesByFullyQualifiedNameAndType()
    val sortedEntities: List<Entity> = entityManager.sortEntitiesByFullyQualifiedName(uniqueEntities)
    val entitiesToMap: Map<String?, Entity?> = sortedEntities.associateBy { it.fullyQualifiedName }
    jacksonObjectMapper()
        .writerWithDefaultPrettyPrinter()
        .writeValue(jsonFile, entitiesToMap)
    println(System.nanoTime() - start)
}

private fun generateKtFile(file: File, encoding: Charset): KtFile {
    val encoded: ByteArray = Files.readAllBytes(Paths.get(file.path))
    val codeString = String(encoded, encoding)
    return PsiFileFactory
            .getInstance(project)
            .createFileFromText(file.path, KotlinFileType.INSTANCE, codeString) as KtFile
}