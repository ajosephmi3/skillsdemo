CREATE TABLE public.person (
	id serial NOT NULL,
	username varchar(100) NOT NULL,
	last_name varchar(100) NULL,
	first_name varchar(100) NULL,
	phone_num varchar(100) NULL,
	email varchar(100) NULL,
	reports_to_id integer NULL,
	"role" varchar(100) NOT NULL,
	account_status int2 NULL,
	created_by varchar(100) NULL,
	created_on timestamp NULL,
	updated_by varchar(100) NULL,
	updated_on timestamp NULL,
	CONSTRAINT person_pkey PRIMARY KEY (id),
	CONSTRAINT person_username_key UNIQUE (username)
);

CREATE TABLE public.person_credential (
	id serial NOT NULL,
	person_id integer NOT NULL,
	"password" varchar(50) NOT NULL,
	CONSTRAINT person_credential_person_id_key UNIQUE (person_id),
	CONSTRAINT person_credential_pkey PRIMARY KEY (id)
);

CREATE TABLE public.project (
	id serial NOT NULL,
	"name" varchar(100) NOT NULL,
	description varchar(3000) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	person_id integer NULL,
	CONSTRAINT project_name_key UNIQUE (name),
	CONSTRAINT project_pkey PRIMARY KEY (id)
);

CREATE TABLE public.person_project (
	id serial NOT NULL,
	person_id integer NULL,
	project_id integer NULL,
	CONSTRAINT person_project_pk PRIMARY KEY (id),
	CONSTRAINT person_project_un UNIQUE (person_id, project_id),
	CONSTRAINT person_project_person_fk FOREIGN KEY (person_id) REFERENCES person(id),
	CONSTRAINT person_project_project_fk FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE public.timesheet (
	id serial NOT NULL,
	person_id int4 NOT NULL,
	start_date date NOT NULL,
	end_date date NOT NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	status varchar(100) NULL,
	submitted_by varchar(100) NULL,
	approved_by varchar(100) NULL,
	submitted_on timestamp NULL,
	approved_on timestamp NULL,
	user_comments varchar NULL,
	approver_comments varchar NULL,
	CONSTRAINT timesheet_pkey PRIMARY KEY (id),
	CONSTRAINT timesheet_un UNIQUE (person_id, start_date),
	CONSTRAINT timesheet_person_fk FOREIGN KEY (person_id) REFERENCES person(id)
);
CREATE INDEX timesheet_person_id_idx ON public.timesheet USING btree (person_id);

CREATE TABLE public.timesheet_line (
	id serial NOT NULL,
	timesheet_id integer NOT NULL,
	project_id integer NULL,
	CONSTRAINT timesheet_line_pkey PRIMARY KEY (id),
	CONSTRAINT timesheet_line_fk FOREIGN KEY (project_id) REFERENCES project(id)
);
CREATE INDEX timesheet_line_timesheet_id_idx ON public.timesheet_line USING btree (timesheet_id);

CREATE TABLE public.time_entry (
	timesheet_line_id integer NOT NULL,
	entry_date date NOT NULL,
	entry_hours numeric(10,2) NULL
);
CREATE INDEX time_entry_timesheet_line_id_idx ON public.time_entry USING btree (timesheet_line_id);

