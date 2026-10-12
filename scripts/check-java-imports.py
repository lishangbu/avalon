#!/usr/bin/env python3
"""检查Java公开类型的文件组织、正文全限定类型及重复导入；语法和类型解析由Maven验证。"""

from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[1]
OPAQUE = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'')
IMPORT = re.compile(r'^import\s+(?:static\s+)?[\w.$]+(?:\.\*)?;', re.M)
QUALIFIED_TYPE = re.compile(r'\b(?:java|javax|jakarta|org|com|io|tools|me)(?:\.[a-z_][a-zA-Z0-9_]*)+\.[A-Z][a-zA-Z0-9_$]*')

# 只识别公开类型声明，允许私有辅助类型和匿名实现留在所属类中。
PUBLIC_TYPE = re.compile(r'\bpublic\s+(?:(?:abstract|static|final|sealed|non-sealed)\s+)*'
                         r'(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)')


def public_type_issues(path, masked):
    """用遮蔽后的花括号层级识别公开成员类型，字符串与注释中的示例不参与检查。"""
    issues = []
    cursor = depth = top_level = 0
    for declaration in PUBLIC_TYPE.finditer(masked):
        prefix = masked[cursor:declaration.start()]
        depth += prefix.count('{') - prefix.count('}')
        cursor = declaration.start()
        line = masked.count('\n', 0, cursor) + 1
        name = declaration.group(1)
        if depth:
            issues.append((path, line, f'公开类型 {name} 应声明在独立文件中'))
        else:
            top_level += 1
            if name != path.stem:
                issues.append((path, line, f'公开类型 {name} 与文件名 {path.name} 不一致'))
    if top_level > 1:
        issues.append((path, 1, '一个Java文件只能声明一个顶层公开类型'))
    return issues


def main():
    issues = []
    files = sorted(path for path in ROOT.glob('avalon-*/**/src/**/*.java')
                   if not {'target', 'node_modules', 'dist'}.intersection(path.parts))
    for path in files:
        source = path.read_text()
        masked = OPAQUE.sub(lambda match: re.sub(r'[^\n]', ' ', match[0]), source)
        issues.extend(public_type_issues(path, masked))
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
    print(f'Java type/import check: {len(files)} files, {len(issues)} issues')
    return bool(issues)


if __name__ == '__main__':
    raise SystemExit(main())
