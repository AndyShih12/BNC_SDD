#!/usr/bin/env python

import sdd

def elements_as_list(node):
    size = sdd.sdd_node_size(node)
    elements = sdd.sdd_node_elements(node)
    return [ sdd.sddNodeArray_getitem(elements,i) for 
             i in xrange(2*size) ]

def models(node,vtree):
    """A generator for the models of an SDD."""
    if sdd.sdd_vtree_is_leaf(vtree):
        var = sdd.sdd_vtree_var(vtree)
        if node is True or sdd.sdd_node_is_true(node):
            yield {var:0}
            yield {var:1}
        elif sdd.sdd_node_is_false(node):
            yield {}
        elif sdd.sdd_node_is_literal(node):
            lit = sdd.sdd_node_literal(node)
            sign = 0 if lit < 0 else 1
            yield {var:sign}
    else:
        left_vtree = sdd.sdd_vtree_left(vtree)
        right_vtree = sdd.sdd_vtree_right(vtree)
        if node is True or sdd.sdd_node_is_true(node):
            # sdd is true
            for left_model in models(True,left_vtree):
                for right_model in models(True,right_vtree):
                    yield _join_models(left_model,right_model)
        elif sdd.sdd_node_is_false(node):
            # sdd is false
            yield {}
        elif sdd.sdd_vtree_of(node) == vtree:
            # enumerate prime/sub pairs
            #elements = sdd.sdd_node_elements(node)
            elements = elements_as_list(node)
            for prime,sub in _pairs(elements):
                if sdd.sdd_node_is_false(sub): continue
                for left_model in models(prime,left_vtree):
                    for right_model in models(sub,right_vtree):
                        yield _join_models(left_model,right_model)
        else: # gap in vtree
            if sdd.sdd_vtree_is_sub(sdd.sdd_vtree_of(node),left_vtree):
                for left_model in models(node,left_vtree):
                    for right_model in models(True,right_vtree):
                        yield _join_models(left_model,right_model)
            else:
                for left_model in models(True,left_vtree):
                    for right_model in models(node,right_vtree):
                        yield _join_models(left_model,right_model)

def _join_models(model1,model2):
    """Join two models."""
    model = model1.copy()
    model.update(model2)
    return model

def _pairs(my_list):
    """A generator for (prime,sub) pairs."""
    if my_list is None: return
    it = iter(my_list)
    for x in it:
        y = it.next()
        yield (x,y)

def str_model(model,var_count=None):
    """Convert model to string."""
    if var_count is None:
        var_count = len(model)
    return " ".join( str(model[var]) for var in xrange(1,var_count+1) )

if __name__ == '__main__':
    var_count = 10
    vtree = sdd.sdd_vtree_new(var_count,"balanced")
    manager = sdd.sdd_manager_new(vtree)

    alpha = sdd.sdd_manager_false(manager)
    for var in xrange(1,var_count+1):
        lit = sdd.sdd_manager_literal(-var,manager)
        alpha = sdd.sdd_disjoin(alpha,lit,manager)

    vt = sdd.sdd_manager_vtree(manager)
    model_count = 0
    for model in models(alpha,vt):
        model_count += 1
        print str_model(model,var_count=var_count)

    #lib_mc = sdd.sdd_model_count(alpha,manager)
    print "model count: %d" % model_count

    sdd.sdd_manager_free(manager)
    sdd.sdd_vtree_free(vtree)
