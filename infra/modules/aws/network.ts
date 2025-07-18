import * as aws from "@pulumi/aws";
import { vpc } from "./network";

export const db = new aws.rds.Instance("haven-db", {
  engine: "postgres",
  engineVersion: "16",
  instanceClass: "db.t3.micro",
  allocatedStorage: 20,
  name: "haven",
  username: "haven",
  password: "ChangeMe123!",
  skipFinalSnapshot: true,
  vpcSecurityGroupIds: [vpc.vpc.defaultSecurityGroupId],
});