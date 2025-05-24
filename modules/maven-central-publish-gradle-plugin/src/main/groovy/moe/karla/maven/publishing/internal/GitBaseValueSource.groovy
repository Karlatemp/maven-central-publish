package moe.karla.maven.publishing.internal

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

import javax.inject.Inject

abstract class GitBaseValueSource implements ValueSource<String, CmdArg> {
    interface CmdArg extends ValueSourceParameters {
        ListProperty<String> getArgs();
    }

    @Inject
    abstract ExecOperations getExecOperations()

    String obtain() {
        ByteArrayOutputStream output = new ByteArrayOutputStream()

        execOperations.exec {
            ArrayList<String> cmd = new ArrayList<>()
            cmd.add("git")
            cmd.addAll(parameters.getArgs().get())
            it.commandLine(cmd)
            it.standardOutput = output
        }
        return output.toString()
    }
}