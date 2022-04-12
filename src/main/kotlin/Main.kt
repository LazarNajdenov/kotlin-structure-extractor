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
        EnvironmentConfigFiles.NATIVE_CONFIG_FILES//Can be JS/NATIVE_CONFIG_FILES for non JVM projects
    ).project
}

fun main(args: Array<String>) {
    val pathToProject: String = System.getProperty("user.dir") + "/projectToAnalyze/"
    val encoding: Charset = Charset.defaultCharset()

    println("Working Directory = $pathToProject")
    File(pathToProject).walk().forEach { file ->
        if (file.name.endsWith(".kt")) {
            val encoded: ByteArray = Files.readAllBytes(Paths.get(file.path))
            val codeString = String(encoded, encoding)
            val ktFile: KtFile = PsiFileFactory
                .getInstance(project)
                .createFileFromText(file.path, KotlinFileType.INSTANCE, codeString) as KtFile
            println("File $file")
            ktFile.children.forEach { psiElement ->
                when (psiElement) {
                    is KtClass -> {
                        println("ClassName: " + psiElement.name)
                        psiElement.children.forEach { child ->
                            when(child) {
                                is KtClassBody -> {
                                    child.functions.forEach { function ->
                                        println("   Function: " + function.name)
                                    }
                                    child.properties.forEach { property ->
                                        println("   Property: " + property.name)
                                    }
                                }
                            }
                        }
                    }
                    is KtNamedFunction -> println("FunctionName: " + psiElement.name)
                    is KtPackageDirective -> println("PackageName: " + psiElement.fqName)
                }
            }


        }

    }

}