"""Check the staged public source tree before building it."""
from pathlib import PurePosixPath
import re
import subprocess
import sys

DOCUMENTS = {
    'README.md', 'CHANGELOG.md', 'CONTRIBUTING.md', 'CODE_OF_CONDUCT.md',
    'SECURITY.md', 'DISTRIBUTION-POLICY.md', 'LICENSE-MODELS.md',
    'docs/SERVER_ADMIN.md', '.github/pull_request_template.md',
}
PROJECT_FILES = DOCUMENTS | {
    '.gitattributes', '.gitignore', 'LICENSE', 'build.gradle', 'settings.gradle',
    'gradle.properties', 'gradlew', 'gradlew.bat', '.github/CODEOWNERS',
    '.github/check-public-source.py', '.github/workflows/build.yml',
    'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties',
}
PRIVATE_PARTS = (
    '/camera/album/', '/textures/gui/album/', '/network/Album',
    '/FrameDesignExchange.java',
)
LOCAL_COMMANDS = {
    'CockatooSpecialCallCommands.java', 'KingfisherFishingCommands.java',
    'KiwiFightCommands.java',
}
RESOURCE_SUFFIXES = {'.json', '.toml', '.mcmeta', '.png', '.ogg', '.fsh', '.vsh'}
LOCAL_JAVA = re.compile(r'Test|Tests|Smoke|(?:^|[a-z])Qa|QA')
PRIVATE_JAVA = re.compile(
    r'camera\.album|AlbumPhotoImport|FrameDesignExchange|PublicAlbum|'
    r'java\.net\.http|org\.junit|@GameTest|porttest|clientqa|'
    r'startRockWalkTest|requestFishingPreview|requestDefecationPreview|'
    r'spawnDebug[A-Z]|DEBUG_BRAIN|Items\.DEBUG_STICK|'
    r'spawnMutation|spawnAllMutations|birdSizeComparison|sizecomparison|'
    r'forceSpecialCall|isSpecialCallForced|startCommandConflict'
)


def permitted_path(name):
    if name in PROJECT_FILES:
        return True
    path = PurePosixPath(name)
    if any(part.startswith('.') for part in path.parts):
        return len(path.parts) == 3 and path.parts[:2] == ('.github', 'ISSUE_TEMPLATE') and path.suffix in {'.yml', '.yaml'}
    if any(part in name for part in PRIVATE_PARTS):
        return False
    if name.startswith('src/main/java/'):
        return path.suffix == '.java' and not LOCAL_JAVA.search(path.name) and path.name not in LOCAL_COMMANDS
    if name.startswith(('src/main/resources/', 'src/generated/resources/')):
        return path.suffix in RESOURCE_SUFFIXES
    return False


def main():
    names = subprocess.check_output(['git', 'ls-files', '-z']).decode('utf-8').split('\0')[:-1]
    errors = []
    for name in names:
        if not permitted_path(name):
            errors.append((name, 'outside the public source allowlist'))
            continue
        if name.endswith('.java'):
            source = subprocess.check_output(['git', 'show', ':' + name]).decode('utf-8-sig')
            if PRIVATE_JAVA.search(source):
                errors.append((name, 'private or local-only implementation'))
    for name, reason in errors:
        print(f'::error::{name!r}: {reason}')
    if errors:
        return 1
    print(f'Public source policy passed: {len(names)} tracked files.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
