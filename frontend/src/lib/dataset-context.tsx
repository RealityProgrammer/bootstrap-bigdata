"use client";

import React, { createContext, useContext, useState, ReactNode } from "react";

interface DatasetContextType {
  currentDataset: string | null;
  setCurrentDataset: (path: string) => void;
  uploadedFiles: Array<{
    name: string;
    path: string;
    uploadedAt: Date;
    size: number;
  }>;
  addUploadedFile: (file: {
    name: string;
    path: string;
    uploadedAt: Date;
    size: number;
  }) => void;
}

const DatasetContext = createContext<DatasetContextType | undefined>(undefined);

export function DatasetProvider({ children }: { children: ReactNode }) {
  const [currentDataset, setCurrentDataset] = useState<string | null>(
    "/data/1.shope_lazada.csv"
  );
  const [uploadedFiles, setUploadedFiles] = useState<
    Array<{
      name: string;
      path: string;
      uploadedAt: Date;
      size: number;
    }>
  >([]);

  const addUploadedFile = (file: {
    name: string;
    path: string;
    uploadedAt: Date;
    size: number;
  }) => {
    setUploadedFiles((prev) => [file, ...prev]);
    setCurrentDataset(file.path); // Auto-select latest uploaded file
  };

  return (
    <DatasetContext.Provider
      value={{
        currentDataset,
        setCurrentDataset,
        uploadedFiles,
        addUploadedFile,
      }}
    >
      {children}
    </DatasetContext.Provider>
  );
}

export function useDataset() {
  const context = useContext(DatasetContext);
  if (context === undefined) {
    throw new Error("useDataset must be used within a DatasetProvider");
  }
  return context;
}
