truncate table nflow_workflow_state;
update nflow_workflow set parent_workflow_id = null, parent_action_id = null;
delete from nflow_workflow_action;
delete from nflow_workflow;
truncate table nflow_executor;

