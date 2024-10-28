package com.astroimagej

import org.gradle.api.GradleException
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

import javax.inject.Inject

// Move away from GRGit as it is may soon stop being maintained
// Wraps the git calls in a value source so that we may catch the error when no git is present
// Used git for ease, but could use JGit if we wanted to remove the git dependency
// There is an issue in that we cannot use a shared BuildService to share the JGit instance,
// and constructing it is expensive
abstract class GitInfo implements ValueSource<String, ValueSourceParameters.None> {
    @Inject
    abstract ExecOperations getExecOperations()

    @Override
    String obtain() {
        try {
            def output = new ByteArrayOutputStream()

            getExecOperations().exec {
                it.commandLine 'git', 'branch', '--show-current'
                it.standardOutput = output
            }

            def branch = output.toString().trim()

            // Get commit hash and repo dirty status
            output = new ByteArrayOutputStream()
            getExecOperations().exec {
                it.commandLine 'git', 'describe', '--always', '--dirty'
                it.standardOutput = output
            }

            def commit = output.toString().trim().replace('-dirty', '+local')

            return "+$branch+$commit"
        } catch (GradleException e) {
            //e.printStackTrace()

            // Git executable was missing, or not executed in a git repo
            return '+no-git'
        }
    }
}