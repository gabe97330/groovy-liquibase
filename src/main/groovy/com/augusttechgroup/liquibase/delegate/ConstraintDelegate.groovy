//
// Groovy Liquibase ChangeLog
//
// Copyright (C) 2010 Tim Berglund
// http://augusttechgroup.com
// Littleton, CO
//
// Licensed under the Apache License 2.0
//

package com.augusttechgroup.liquibase.delegate

import liquibase.change.ConstraintsConfig


class ConstraintDelegate {
  def constraint


  ConstraintDelegate() {
    constraint = new ConstraintsConfig(primaryKey: false,
                                       primaryKeyName: null,
                                       primaryKeyTablespace: null,
                                       foreignKeyName: null,
                                       references: null,
                                       check: null,
                                       unique: false,
                                       uniqueConstraintName: null,
                                       deleteCascade: false,
                                       initiallyDeferred: false,
                                       deferrable: false,
                                       nullable: true)
  }


  def constraints(Map params = [:]) {
    params.each { key, value ->
      constraint[key] = value
    }
  }


  def constraints(Closure closure) {
    closure.delegate = this
    closure.call()
  }


  def methodMissing(String name, params) {
    if(constraint.hasProperty(name)) {
      constraint[name] = params[0]
    }
  }
}