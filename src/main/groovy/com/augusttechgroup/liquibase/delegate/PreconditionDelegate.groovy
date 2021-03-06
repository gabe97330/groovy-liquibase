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

import liquibase.precondition.core.AndPrecondition
import liquibase.precondition.core.OrPrecondition
import liquibase.precondition.core.SqlPrecondition
import liquibase.precondition.CustomPreconditionWrapper
import liquibase.precondition.PreconditionFactory
import liquibase.precondition.core.PreconditionContainer
import liquibase.precondition.core.PreconditionContainer.OnSqlOutputOption
import liquibase.precondition.core.PreconditionContainer.ErrorOption
import liquibase.precondition.core.PreconditionContainer.FailOption


class PreconditionDelegate
{
  def preconditions = []
  

  //
  // Handles all non-nesting preconditions using the PreconditionFactory.
  //
  void methodMissing(String name, args) {
    def preconditionFactory = PreconditionFactory.instance
    def precondition = preconditionFactory.create(name)
    def params = args[0]

    if(params != null && params instanceof Map) {
      args[0].each { key, value ->
        precondition[key] = value
      }
    }

    preconditions << precondition
  }


  def sqlCheck(Map params = [:], Closure closure) {
    def precondition = new SqlPrecondition()
    precondition.expectedResult = params.expectedResult
    precondition.sql = closure.call()
    preconditions << precondition
  }


  def customPrecondition(Map params = [:], Closure closure) {
    def delegate = new KeyValueDelegate()
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_ONLY
    closure.call()

    def precondition = new CustomPreconditionWrapper()
    precondition.className = params.className
    delegate.map.each { key, value ->
      precondition.setParam(key, value.toString())
    }

    preconditions << precondition
  }

  
  def and(Closure closure) {
    def precondition = nestedPrecondition(AndPrecondition, closure)
    preconditions << precondition
  }


  def or(Closure closure) {
    def precondition = nestedPrecondition(OrPrecondition, closure)
    preconditions << precondition
  }


  static PreconditionContainer buildPreconditionContainer(Map params, Closure closure) {
    def preconditions = new PreconditionContainer()

    if(params.onFail) {
      preconditions.onFail = FailOption."${params.onFail}"
    }

    if(params.onError) {
      preconditions.onError = ErrorOption."${params.onError}"
    }

    if(params.onUpdateSQL) {
      preconditions.onSqlOutput = OnSqlOutputOption."${params.onUpdateSQL}"
    }

    preconditions.onFailMessage = params.onFailMessage
    preconditions.onErrorMessage = params.onErrorMessage

    def delegate = new PreconditionDelegate()
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()

    delegate.preconditions.each { precondition ->
      preconditions.addNestedPrecondition(precondition)
    }

    return preconditions
  }


  private def nestedPrecondition(Class preconditionClass, Closure closure) {
    def nestedPrecondition = preconditionClass.newInstance()
    def delegate = new PreconditionDelegate()
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_ONLY
    closure.call()

    delegate.preconditions.each { precondition ->
      nestedPrecondition.addNestedPrecondition(precondition)
    }

    return nestedPrecondition
  }

}