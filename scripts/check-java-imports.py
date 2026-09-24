#!/usr/bin/env python3
"""检查源码正文中的全限定类型及重复导入；类型解析仍由 Maven 编译验证。"""

from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[1]
OPAQUE = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'')
IMPORT = re.compile(r'^import\s+(?:static\s+)?[\w.$]+(?:\.\*)?;', re.M)
QUALIFIED_TYPE = re.compile(r'\b(?:java|javax|jakarta|org|com|io|tools|me)(?:\.[a-z_][a-zA-Z0-9_]*)+\.[A-Z][a-zA-Z0-9_$]*')


def main():
    issues = []
    files = sorted(path for path in ROOT.glob('avalon-*/**/src/**/*.java')
                   if not {'target', 'node_modules', 'dist'}.intersection(path.parts))
    for path in files:
        source = path.read_text()
        masked = OPAQUE.sub(lambda match: re.sub(r'[^\n]', ' ', match[0]), source)
        seen = set()
        for match in IMPORT.finditer(masked):
            statement = ' '.join(match[0].split())
            if statement in seen:
                issues.append((path, masked.count('\n', 0, match.start()) + 1, statement))
            seen.add(statement)
        body = re.sub(r'^(?:package|import)\s+[^;]+;',
                      lambda match: re.sub(r'[^\n]', ' ', match[0]), masked, flags=re.M)
        for match in QUALIFIED_TYPE.finditer(body):
            issues.append((path, body.count('\n', 0, match.start()) + 1, match[0]))
    for path, line, value in issues:
        print(f'{path.relative_to(ROOT)}:{line}: {value}')
    print(f'Java import check: {len(files)} files, {len(issues)} issues')
    return bool(issues)


if __name__ == '__main__':
    raise SystemExit(main())
