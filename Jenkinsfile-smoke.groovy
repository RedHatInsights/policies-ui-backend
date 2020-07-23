/*
 *
 * Requires: https://github.com/RedHatInsights/insights-pipeline-lib
 *
 */

@Library("github.com/RedHatInsights/insights-pipeline-lib@v3") _

def options = [vaultEnabled: true, settingsFromGit: true, settingsGitPath: 'configs/policies-smoke-settings.yaml']

if (env.CHANGE_ID) {
        execSmokeTest (
        ocDeployerBuilderPath: "policies/policies-ui-backend",
        ocDeployerComponentPath: "policies/policies-ui-backend",
        ocDeployerServiceSets: "policies,platform-mq, rbac",
        iqePlugins: ["iqe-policies-plugin"],
        pytestMarker: "policies_api_smoke",
        options: options
    )
}
