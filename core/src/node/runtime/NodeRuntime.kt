//package node.runtime
//
//import org.jetbrains.jet.config.CompilerConfiguration
//import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil
//import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
//import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
//import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentException
//import java.io.File
//import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
//import org.jetbrains.jet.utils.PathUtil
//import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream
//import org.jetbrains.jet.cli.common.CLIConfigurationKeys
//import java.util.ArrayList
//import org.apache.ivy.Ivy
//import org.apache.ivy.core.module.descriptor.ModuleDescriptor
//import org.apache.ivy.core.settings.IvySettings
//import org.apache.ivy.util.Message
//import org.apache.ivy.core.retrieve.RetrieveOptions
//import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser
//
//fun compile(src: String, output: String, lib: String, classPath: List<String>) {
//  try {
//    val environment = env(lib, classPath, arrayListOf(src))
//
//    val success = KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, null, File(output), true)
//    if (!success) {
//      throw CompileEnvironmentException(errorMessage(src, false))
//    }
//  } catch (e: Exception) {
//    throw CompileEnvironmentException(errorMessage(src, true), e)
//  }
//}
//
//fun env(stdLib: String, classPath: List<String>, srcRoots: List<String>): JetCoreEnvironment {
//  val configuration = createConfiguration(stdLib, classPath, srcRoots);
//
//  return JetCoreEnvironment(CompileEnvironmentUtil.createMockDisposable(), configuration);
//}
//
//fun errorMessage(source: String, exceptionThrown: Boolean): String {
//  val format = "[%s] compilation failed" + (if (exceptionThrown) "" else ", see \"ERROR:\" messages above for more details.")
//  return java.lang.String.format(format, File(source).getAbsolutePath())
//}
//
//fun createConfiguration(stdLib: String, classPath: List<String>, srcRoots: List<String>): CompilerConfiguration {
//  val configuration = CompilerConfiguration()
//
//  configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, PathUtil.findRtJar())
//  configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, File(stdLib))
//
//  for (path in classPath) {
//    configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, File(path))
//  }
//
//  configuration.addAll(org.jetbrains.jet.config.CommonConfigurationKeys.SOURCE_ROOTS_KEY, srcRoots)
//  configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR)
//
//  return configuration
//}
//
//fun resolve() {
////  val settings = IvySettings()
////
////  settings.setDefaultIvyUserDir(File(".repo"))
////
////  settings.setDefaultCache(File("D:/programming/eclipse_projects/ivyTest/repo/cache/"));
////  settings.setDefaultCacheArtifactPattern("[module]/[revision]/[module]-[revision](-[classifier]");
////
////  val ivy = Ivy.newInstance(settings)!!
////
////  ivy.getLoggerEngine()!!.pushLogger(DefaultMessageLogger(Message.MSG_VERBOSE));
////
////  val retriveOptions = RetrieveOptions()
////  retriveOptions.setUseOrigin(true);
////  retriveOptions.setConfs(md.getConfigurationsNames());
////  ivy.retrieve(md.getModuleRevisionId(), "lib/[conf]/[artifact].[ext]", retriveOptions);
//}
//
//fun main(args: Array<String>) {
//  compile(args[0], args[1], args[2], ArrayList<String>())
//}