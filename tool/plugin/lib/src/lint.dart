// Copyright 2017 The Chromium Authors. All rights reserved. Use of this source
// code is governed by a BSD-style license that can be found in the LICENSE file.

import 'dart:async';
import 'dart:io';

import 'package:args/command_runner.dart';

import '../plugin.dart';

class LintCommand extends Command {
  final BuildCommandRunner runner;

  LintCommand(this.runner);

  String get name => 'lint';

  String get description =>
      'Perform simple validations on the flutter-intellij repo code.';

  Future<int> run() async {
    // Print a report for the API used from the Dart plugin.
    printApiUsage();

    // Check for unintentionally imported annotations.
    if (checkForBadImports()) {
      return 1;
    }

    return 0;
  }

  void printApiUsage() {
    final ProcessResult result = Process.runSync(
      'git',
      // Note: extra quotes added so grep doesn't match this file.
      ['grep', 'import com.jetbrains.' 'lang.dart.'],
    );
    final String imports = result.stdout.trim();

    // path:import
    final Map<String, List<String>> usages = {};

    imports.split('\n').forEach((String line) {
      if (line
          .trim()
          .isEmpty) {
        return;
      }

      int index = line.indexOf(':');
      String place = line.substring(0, index);
      String import = line.substring(index + 1);
      if (import.startsWith('import ')) import = import.substring(7);
      if (import.endsWith(';')) import = import.substring(0, import.length - 1);
      usages.putIfAbsent(import, () => []);
      usages[import].add(place);
    });

    // print report
    final List<String> keys = usages.keys.toList();
    keys.sort();

    print('${keys.length} separate Dart plugin APIs used:');
    print('------');

    for (String import in keys) {
      print('$import:');
      List<String> places = usages[import];
      places.forEach((String place) => print('  $place'));
      print('');
    }
  }

  /// Return `true` if an import violation was found.
  bool checkForBadImports() {
    final List<String> proscribedImports = [
      'com.android.annotations.NonNull',
      'javax.annotation.Nullable'
    ];

    for (String import in proscribedImports) {
      print('Checking for import of "$import"...');

      final ProcessResult result = Process.runSync(
        'git',
        ['grep', 'import $import;'],
      );

      final String results = result.stdout.trim();
      if (results.isNotEmpty) {
        print('Found proscribed imports:\n');
        print(results);
        return true;
      } else {
        print('  none found');
      }
    }

    return false;
  }
}